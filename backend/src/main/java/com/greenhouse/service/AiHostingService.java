package com.greenhouse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.entity.*;
import com.greenhouse.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * AI自动托管服务
 * 每10分钟自动检查传感器数据并执行相应的控制操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiHostingService {
    
    private final AiHostingConfigMapper configMapper;
    private final AiHostingLogMapper logMapper;
    private final SensorDataMapper sensorDataMapper;
    private final AutomationSettingMapper automationSettingMapper;
    private final PlotMapper plotMapper;
    private final RecipeMapper recipeMapper;
    private final PlotAssignmentMapper plotAssignmentMapper;
    private final MqttService mqttService;
    private final EmailService emailService;
    private final AiService aiService;
    private final com.greenhouse.mapper.ImageMapper imageMapper;
    private final TaskScheduler taskScheduler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private volatile boolean isRunning = false;
    private ScheduledFuture<?> scheduledTask;
    private static final String MQTT_TOPIC = "voluntarilyAi";
    
    /**
     * 初始化服务，启动定时任务
     */
    @PostConstruct
    public void init() {
        log.info("AI托管服务初始化，启动定时任务");
        scheduleTask();
    }
    
    /**
     * 销毁服务，停止定时任务
     */
    @PreDestroy
    public void destroy() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
            log.info("AI托管定时任务已停止");
        }
    }
    
    /**
     * 根据配置的检查间隔调度任务
     */
    private void scheduleTask() {
        // 取消现有任务
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
            log.info("已取消现有的AI托管定时任务");
        }
        
        // 获取配置
        AiHostingConfig config = getConfig();
        
        // 如果功能未启用，不调度任务
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            log.info("AI托管功能未启用，不调度定时任务");
            return;
        }
        
        int intervalMinutes = config.getCheckIntervalMinutes() != null 
            ? config.getCheckIntervalMinutes() 
            : 10; // 默认10分钟
        
        // 确保间隔至少为1分钟
        if (intervalMinutes < 1) {
            intervalMinutes = 1;
            log.warn("检查间隔不能小于1分钟，已设置为1分钟");
        }
        
        // 创建新的定时任务
        PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMinutes(intervalMinutes));
        trigger.setInitialDelay(Duration.ofMinutes(intervalMinutes)); // 首次延迟
        
        scheduledTask = taskScheduler.schedule(this::executeHosting, trigger);
        log.info("AI托管定时任务已调度，检查间隔: {} 分钟", intervalMinutes);
    }
    
    /**
     * 重新调度任务（当配置更新时调用）
     */
    public void rescheduleTask() {
        log.info("重新调度AI托管定时任务");
        scheduleTask();
    }
    
    /**
     * 执行AI托管检查
     */
    @Transactional
    public void executeHosting() {
        // 防止并发执行
        if (isRunning) {
            log.warn("AI托管任务正在执行中，跳过本次执行");
            return;
        }
        
        try {
            isRunning = true;
            
            // 获取配置
            AiHostingConfig config = getConfig();
            if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
                log.debug("AI托管功能未启用，跳过执行");
                return;
            }
            
            long startTime = System.currentTimeMillis();
            log.info("开始执行AI自动托管任务");
            
            List<String> actions = new ArrayList<>();
            List<String> issues = new ArrayList<>();
            String status = "success";
            String errorMessage = null;
            String aiAnalysis = null;
            String aiSummary = null;
            
            try {
                // 1. AI分析：获取今天最新的10张图片和30条温度记录
                List<com.greenhouse.entity.Image> todayImages = imageMapper.findTodayLatestImages(10);
                List<SensorData> todayTemperatureRecords = sensorDataMapper.findTodayLatestTemperatureRecords(30);
                
                log.info("AI分析：获取到{}张图片，{}条温度记录", todayImages.size(), todayTemperatureRecords.size());
                
                // 2. 调用AI进行分析
                Map<String, Object> aiResult = null;
                try {
                    aiResult = aiService.analyzeForHosting(todayImages, todayTemperatureRecords);
                    aiAnalysis = (String) aiResult.getOrDefault("analysis", "");
                    aiSummary = (String) aiResult.getOrDefault("summary", "");
                    
                    // 添加AI识别的问题
                    @SuppressWarnings("unchecked")
                    List<String> aiIssues = (List<String>) aiResult.getOrDefault("issues", new ArrayList<>());
                    issues.addAll(aiIssues);
                    
                    log.info("AI分析完成，识别到{}个问题", aiIssues.size());
                } catch (Exception e) {
                    log.error("AI分析失败: {}", e.getMessage(), e);
                    issues.add("AI分析失败: " + e.getMessage());
                    aiAnalysis = "AI分析服务异常，使用传统规则分析";
                }
                
                // 3. 获取最新传感器数据
                SensorData latestSensor = sensorDataMapper.selectList(null).stream()
                    .max(Comparator.comparing(SensorData::getRecordTime))
                    .orElse(null);
                
                if (latestSensor == null) {
                    issues.add("未找到传感器数据");
                    status = "partial";
                } else {
                    // 4. 获取自动化设置
                    Map<String, Object> automationSettings = getAutomationSettings();
                    
                    // 5. 根据AI建议执行控制操作（优先使用AI建议）
                    if (aiResult != null) {
                        executeAiRecommendations(aiResult, config, actions, issues);
                    }
                    
                    // 6. 传统规则分析并执行控制操作（作为补充）
                    analyzeAndControl(latestSensor, automationSettings, config, actions, issues);
                }
                
            } catch (Exception e) {
                log.error("AI托管执行失败: {}", e.getMessage(), e);
                status = "failed";
                errorMessage = e.getMessage();
                issues.add("执行异常: " + e.getMessage());
            }
            
            // 7. 发送邮件报警（如果有问题或AI分析结果）
            boolean emailSent = false;
            String emailContent = null;
            if (Boolean.TRUE.equals(config.getEmailEnabled()) && 
                config.getEmailAddress() != null && !config.getEmailAddress().isEmpty()) {
                if (!issues.isEmpty() || !actions.isEmpty() || aiAnalysis != null) {
                    emailSent = emailService.sendHostingReportEmail(
                        config.getEmailAddress(),
                        status,
                        actions,
                        issues,
                        aiAnalysis,
                        aiSummary
                    );
                    emailContent = buildEmailContent(status, actions, issues, aiAnalysis, aiSummary);
                }
            }
            
            // 8. 记录执行日志
            long duration = System.currentTimeMillis() - startTime;
            saveExecutionLog(status, actions, issues, emailSent, emailContent, duration, errorMessage, aiAnalysis, aiSummary);
            
            log.info("AI自动托管任务执行完成，状态: {}, 耗时: {}ms", status, duration);
            
        } finally {
            isRunning = false;
        }
    }
    
    /**
     * 获取AI托管配置
     */
    private AiHostingConfig getConfig() {
        List<AiHostingConfig> configs = configMapper.selectList(null);
        if (configs.isEmpty()) {
            // 创建默认配置
            AiHostingConfig config = new AiHostingConfig();
            config.setEnabled(false);
            config.setEmailEnabled(true);
            config.setCheckIntervalMinutes(10);
            config.setWaterControlEnabled(true);
            config.setLightControlEnabled(true);
            config.setRecipeExecutionEnabled(true);
            config.setCreatedAt(new Date());
            config.setUpdatedAt(new Date());
            configMapper.insert(config);
            return config;
        }
        return configs.get(0);
    }
    
    /**
     * 获取自动化设置
     */
    private Map<String, Object> getAutomationSettings() {
        Map<String, Object> settings = new HashMap<>();
        List<AutomationSetting> allSettings = automationSettingMapper.selectList(null);
        for (AutomationSetting setting : allSettings) {
            String key = setting.getSettingKey();
            String value = setting.getSettingValue();
            
            // 尝试解析为数字或布尔值
            if ("true".equals(value) || "false".equals(value)) {
                settings.put(key, Boolean.parseBoolean(value));
            } else {
                try {
                    settings.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    settings.put(key, value);
                }
            }
        }
        return settings;
    }
    
    /**
     * 执行AI建议的控制操作
     */
    @SuppressWarnings("unchecked")
    private void executeAiRecommendations(Map<String, Object> aiResult, AiHostingConfig config, 
                                         List<String> actions, List<String> issues) {
        try {
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) aiResult.getOrDefault("recommendations", new ArrayList<>());
            
            for (Map<String, Object> rec : recommendations) {
                String type = (String) rec.get("type");
                String action = (String) rec.get("action");
                String reason = (String) rec.getOrDefault("reason", "AI建议");
                
                try {
                    if ("light".equals(type)) {
                        if (Boolean.TRUE.equals(config.getLightControlEnabled())) {
                            Map<String, Object> message = new HashMap<>();
                            message.put("operationType", 1);
                            message.put("operation", "light");
                            message.put("action", "on".equals(action) ? "on" : "off");
                            message.put("reason", "AI建议: " + reason);
                            message.put("source", "ai_hosting");
                            
                            String jsonMessage = objectMapper.writeValueAsString(message);
                            mqttService.publish(MQTT_TOPIC, jsonMessage, 1);
                            
                            actions.add("AI建议 - " + ("on".equals(action) ? "开启" : "关闭") + "补光灯: " + reason);
                            log.info("AI托管: {}补光灯 - {}", "on".equals(action) ? "开启" : "关闭", reason);
                        }
                    } else if ("water".equals(type)) {
                        if (Boolean.TRUE.equals(config.getWaterControlEnabled())) {
                            Map<String, Object> message = new HashMap<>();
                            message.put("operationType", 2);
                            message.put("operation", "water");
                            message.put("action", "start".equals(action) ? "start" : "stop");
                            message.put("reason", "AI建议: " + reason);
                            message.put("source", "ai_hosting");
                            
                            String jsonMessage = objectMapper.writeValueAsString(message);
                            mqttService.publish(MQTT_TOPIC, jsonMessage, 1);
                            
                            actions.add("AI建议 - " + ("start".equals(action) ? "启动" : "停止") + "抽水: " + reason);
                            log.info("AI托管: {}抽水 - {}", "start".equals(action) ? "启动" : "停止", reason);
                        }
                    } else if ("recipe".equals(type)) {
                        if (Boolean.TRUE.equals(config.getRecipeExecutionEnabled())) {
                            Integer plotId = rec.get("plotId") != null ? Integer.parseInt(rec.get("plotId").toString()) : null;
                            Integer recipeId = rec.get("recipeId") != null ? Integer.parseInt(rec.get("recipeId").toString()) : null;
                            Integer executions = rec.get("executions") != null ? Integer.parseInt(rec.get("executions").toString()) : 1;
                            
                            if (plotId != null && recipeId != null) {
                                Plot plot = plotMapper.selectById(plotId);
                                Recipe recipe = recipeMapper.selectById(recipeId);
                                
                                if (plot != null && recipe != null) {
                                    Map<String, Object> message = new HashMap<>();
                                    message.put("operationType", 3);
                                    message.put("plotId", plot.getPlotNumber());
                                    message.put("plotName", plot.getName() != null ? plot.getName() : "地块" + plot.getPlotNumber());
                                    message.put("recipeId", recipe.getId());
                                    message.put("recipeName", recipe.getName());
                                    message.put("waterMl", recipe.getWaterMl() != null ? recipe.getWaterMl() : 0);
                                    message.put("nutrientMl", recipe.getNutrientMl() != null ? recipe.getNutrientMl() : 0);
                                    message.put("rootingPowderMl", recipe.getRootingPowderMl() != null ? recipe.getRootingPowderMl() : 0);
                                    message.put("specialMl", recipe.getSpecialMl() != null ? recipe.getSpecialMl() : 0);
                                    message.put("executions", executions);
                                    message.put("reason", "AI建议: " + reason);
                                    message.put("source", "ai_hosting");
                                    
                                    String jsonMessage = objectMapper.writeValueAsString(message);
                                    mqttService.publish(MQTT_TOPIC, jsonMessage, 1);
                                    
                                    actions.add("AI建议 - 执行配方: 地块" + plot.getPlotNumber() + "(" + recipe.getName() + ") - " + reason);
                                    log.info("AI托管: 执行配方，地块: {}, 配方: {}", plot.getPlotNumber(), recipe.getName());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("执行AI建议失败: type={}, action={}, error={}", type, action, e.getMessage(), e);
                    issues.add("执行AI建议失败 - " + type + "/" + action + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("解析AI建议失败: {}", e.getMessage(), e);
            issues.add("解析AI建议失败: " + e.getMessage());
        }
    }
    
    /**
     * 分析数据并执行控制操作
     */
    private void analyzeAndControl(SensorData sensor, Map<String, Object> automationSettings, 
                                   AiHostingConfig config, List<String> actions, List<String> issues) {
        
        // 检查光照并控制补光
        if (Boolean.TRUE.equals(config.getLightControlEnabled())) {
            checkAndControlLight(sensor, automationSettings, actions, issues);
        }
        
        // 检查土壤湿度并控制水
        if (Boolean.TRUE.equals(config.getWaterControlEnabled())) {
            checkAndControlWater(sensor, automationSettings, actions, issues);
        }
        
        // 检查并执行土壤配方
        if (Boolean.TRUE.equals(config.getRecipeExecutionEnabled())) {
            checkAndExecuteRecipe(sensor, automationSettings, actions, issues);
        }
        
        // 检查其他异常情况
        checkOtherIssues(sensor, automationSettings, issues);
    }
    
    /**
     * 检查并控制补光
     */
    private void checkAndControlLight(SensorData sensor, Map<String, Object> settings, 
                                      List<String> actions, List<String> issues) {
        Integer lightLux = sensor.getLightLux() != null ? sensor.getLightLux().intValue() : 0;
        Integer threshold = (Integer) settings.getOrDefault("lightLuxThreshold", 8000);
        Boolean autoLightEnabled = (Boolean) settings.getOrDefault("autoLightEnabled", true);
        
        if (!Boolean.TRUE.equals(autoLightEnabled)) {
            return;
        }
        
        if (lightLux < threshold) {
            // 光照不足，需要开灯
            try {
                Map<String, Object> message = new HashMap<>();
                message.put("operationType", 1);
                message.put("operation", "light");
                message.put("action", "on");
                message.put("reason", "光照强度(" + lightLux + " lux)低于阈值(" + threshold + " lux)");
                message.put("source", "ai_hosting");
                
                String jsonMessage = objectMapper.writeValueAsString(message);
                mqttService.publish(MQTT_TOPIC, jsonMessage, 1);
                
                actions.add("开启补光灯 - 光照强度(" + lightLux + " lux)低于阈值(" + threshold + " lux)");
                log.info("AI托管: 开启补光灯，当前光照: {} lux", lightLux);
            } catch (Exception e) {
                log.error("发送补光灯控制消息失败: {}", e.getMessage(), e);
                issues.add("补光灯控制失败: " + e.getMessage());
            }
        } else {
            // 光照充足，可以关灯
            try {
                Map<String, Object> message = new HashMap<>();
                message.put("operationType", 1);
                message.put("operation", "light");
                message.put("action", "off");
                message.put("reason", "光照强度(" + lightLux + " lux)已达到阈值(" + threshold + " lux)");
                message.put("source", "ai_hosting");
                
                String jsonMessage = objectMapper.writeValueAsString(message);
                mqttService.publish(MQTT_TOPIC, jsonMessage, 1);
                
                actions.add("关闭补光灯 - 光照强度(" + lightLux + " lux)已达到阈值");
                log.info("AI托管: 关闭补光灯，当前光照: {} lux", lightLux);
            } catch (Exception e) {
                log.error("发送补光灯控制消息失败: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 检查并控制水
     */
    private void checkAndControlWater(SensorData sensor, Map<String, Object> settings, 
                                     List<String> actions, List<String> issues) {
        Double soilMoisture = sensor.getSoilMoisturePct() != null ? sensor.getSoilMoisturePct().doubleValue() : 0.0;
        Integer threshold = (Integer) settings.getOrDefault("soilMoistureLowThreshold", 35);
        Boolean autoPumpEnabled = (Boolean) settings.getOrDefault("autoPumpEnabled", true);
        
        if (!Boolean.TRUE.equals(autoPumpEnabled)) {
            return;
        }
        
        if (soilMoisture < threshold) {
            // 土壤湿度低，需要抽水
            try {
                Map<String, Object> message = new HashMap<>();
                message.put("operationType", 2);
                message.put("operation", "water");
                message.put("action", "start");
                message.put("reason", "土壤湿度(" + String.format("%.1f", soilMoisture) + "%)低于阈值(" + threshold + "%)");
                message.put("source", "ai_hosting");
                
                String jsonMessage = objectMapper.writeValueAsString(message);
                mqttService.publish(MQTT_TOPIC, jsonMessage, 1);
                
                actions.add("启动抽水 - 土壤湿度(" + String.format("%.1f", soilMoisture) + "%)低于阈值(" + threshold + "%)");
                log.info("AI托管: 启动抽水，当前土壤湿度: {}%", soilMoisture);
            } catch (Exception e) {
                log.error("发送抽水控制消息失败: {}", e.getMessage(), e);
                issues.add("抽水控制失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 检查并执行土壤配方
     */
    private void checkAndExecuteRecipe(SensorData sensor, Map<String, Object> settings, 
                                       List<String> actions, List<String> issues) {
        // 获取所有地块的配方分配
        List<PlotAssignment> assignments = plotAssignmentMapper.selectList(null).stream()
            .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
            .collect(Collectors.toList());
        
        for (PlotAssignment assignment : assignments) {
            try {
                Plot plot = plotMapper.selectById(assignment.getPlotId());
                Recipe recipe = recipeMapper.selectById(assignment.getRecipeId());
                
                if (plot == null || recipe == null) {
                    continue;
                }
                
                // 检查是否需要执行配方（可以根据传感器数据判断，这里简化处理，定期执行）
                // 可以根据土壤湿度、温度等条件判断
                Double soilMoisture = sensor.getSoilMoisturePct() != null ? sensor.getSoilMoisturePct().doubleValue() : 0.0;
                
                // 如果土壤湿度较低，执行配方
                if (soilMoisture < 40) {
                    Map<String, Object> message = new HashMap<>();
                    message.put("operationType", 3);
                    message.put("plotId", plot.getPlotNumber());
                    message.put("plotName", plot.getName() != null ? plot.getName() : "地块" + plot.getPlotNumber());
                    message.put("recipeId", recipe.getId());
                    message.put("recipeName", recipe.getName());
                    message.put("waterMl", recipe.getWaterMl() != null ? recipe.getWaterMl() : 0);
                    message.put("nutrientMl", recipe.getNutrientMl() != null ? recipe.getNutrientMl() : 0);
                    message.put("rootingPowderMl", recipe.getRootingPowderMl() != null ? recipe.getRootingPowderMl() : 0);
                    message.put("specialMl", recipe.getSpecialMl() != null ? recipe.getSpecialMl() : 0);
                    message.put("executions", 1);
                    message.put("reason", "AI托管自动执行 - 土壤湿度(" + String.format("%.1f", soilMoisture) + "%)较低");
                    message.put("source", "ai_hosting");
                    
                    String jsonMessage = objectMapper.writeValueAsString(message);
                    mqttService.publish(MQTT_TOPIC, jsonMessage, 1);
                    
                    actions.add("执行配方 - 地块" + plot.getPlotNumber() + "(" + recipe.getName() + ")");
                    log.info("AI托管: 执行配方，地块: {}, 配方: {}", plot.getPlotNumber(), recipe.getName());
                }
            } catch (Exception e) {
                log.error("执行配方失败，地块ID: {}, 配方ID: {}, 错误: {}", 
                    assignment.getPlotId(), assignment.getRecipeId(), e.getMessage(), e);
                issues.add("执行配方失败 - 地块" + assignment.getPlotId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * 检查其他异常情况
     */
    private void checkOtherIssues(SensorData sensor, Map<String, Object> settings, List<String> issues) {
        // 检查温度
        Double temperature = sensor.getTemperatureC() != null ? sensor.getTemperatureC().doubleValue() : 0.0;
        Integer tempHigh = (Integer) settings.getOrDefault("temperatureHighThreshold", 35);
        Integer tempLow = (Integer) settings.getOrDefault("temperatureLowThreshold", 10);
        
        if (temperature > tempHigh) {
            issues.add("温度过高: " + String.format("%.1f", temperature) + "°C (阈值: " + tempHigh + "°C)");
        }
        if (temperature < tempLow) {
            issues.add("温度过低: " + String.format("%.1f", temperature) + "°C (阈值: " + tempLow + "°C)");
        }
        
        // 检查湿度
        Double humidity = sensor.getHumidityPct() != null ? sensor.getHumidityPct().doubleValue() : 0.0;
        Integer humidityHigh = (Integer) settings.getOrDefault("humidityHighThreshold", 80);
        Integer humidityLow = (Integer) settings.getOrDefault("humidityLowThreshold", 30);
        
        if (humidity > humidityHigh) {
            issues.add("湿度过高: " + String.format("%.1f", humidity) + "% (阈值: " + humidityHigh + "%)");
        }
        if (humidity < humidityLow) {
            issues.add("湿度过低: " + String.format("%.1f", humidity) + "% (阈值: " + humidityLow + "%)");
        }
        
        // 检查氧气
        Double oxygen = sensor.getOxygenPct() != null ? sensor.getOxygenPct().doubleValue() : 0.0;
        Double oxygenLow = ((Number) settings.getOrDefault("oxygenLowThreshold", 18)).doubleValue();
        
        if (oxygen < oxygenLow) {
            issues.add("氧气含量过低: " + String.format("%.1f", oxygen) + "% (阈值: " + oxygenLow + "%)");
        }
        
        // 检查二氧化碳
        Integer co2 = sensor.getCo2Ppm() != null ? sensor.getCo2Ppm() : 0;
        Integer co2High = (Integer) settings.getOrDefault("co2HighThreshold", 1000);
        
        if (co2 > co2High) {
            issues.add("二氧化碳含量过高: " + co2 + " ppm (阈值: " + co2High + " ppm)");
        }
    }
    
    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String status, List<String> actions, List<String> issues, 
                                     String aiAnalysis, String aiSummary) {
        StringBuilder content = new StringBuilder();
        content.append("智能温室AI自动托管执行报告\n\n");
        content.append("执行状态: ").append(status).append("\n\n");
        
        // AI分析结果
        if (aiSummary != null && !aiSummary.isEmpty()) {
            content.append("AI分析总结: ").append(aiSummary).append("\n\n");
        }
        
        if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
            content.append("AI详细分析:\n").append(aiAnalysis).append("\n\n");
        }
        
        if (!actions.isEmpty()) {
            content.append("执行的操作:\n");
            for (int i = 0; i < actions.size(); i++) {
                content.append(i + 1).append(". ").append(actions.get(i)).append("\n");
            }
            content.append("\n");
        }
        
        if (!issues.isEmpty()) {
            content.append("检测到的问题:\n");
            for (int i = 0; i < issues.size(); i++) {
                content.append(i + 1).append(". ").append(issues.get(i)).append("\n");
            }
            content.append("\n");
        }
        
        content.append("请及时查看系统状态并采取相应措施。\n");
        return content.toString();
    }
    
    /**
     * 保存执行日志
     */
    private void saveExecutionLog(String status, List<String> actions, List<String> issues, 
                                  boolean emailSent, String emailContent, long duration, String errorMessage,
                                  String aiAnalysis, String aiSummary) {
        try {
            AiHostingLog log = new AiHostingLog();
            log.setExecutionTime(new Date());
            log.setStatus(status);
            
            try {
                log.setActionsTaken(objectMapper.writeValueAsString(actions));
            } catch (Exception e) {
                log.setActionsTaken("[]");
            }
            
            try {
                log.setIssuesDetected(objectMapper.writeValueAsString(issues));
            } catch (Exception e) {
                log.setIssuesDetected("[]");
            }
            
            log.setEmailSent(emailSent);
            log.setEmailContent(emailContent);
            log.setExecutionDurationMs((int) duration);
            log.setErrorMessage(errorMessage);
            log.setCreatedAt(new Date());
            
            logMapper.insert(log);
        } catch (Exception e) {
            log.error("保存AI托管执行日志失败: {}", e.getMessage(), e);
        }
    }
}

