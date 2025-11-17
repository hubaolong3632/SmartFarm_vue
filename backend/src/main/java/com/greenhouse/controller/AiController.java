package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.AiReportDTO;
import com.greenhouse.entity.AiReport;
import com.greenhouse.entity.Image;
import com.greenhouse.entity.SensorData;
import com.greenhouse.mapper.AiReportMapper;
import com.greenhouse.mapper.ImageMapper;
import com.greenhouse.mapper.SensorDataMapper;
import com.greenhouse.mapper.ExecutionLogMapper;
import com.greenhouse.mapper.AutomationSettingMapper;
import com.greenhouse.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * AI分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AiController {
    
    private final AiService aiService;
    private final ImageMapper imageMapper;
    private final SensorDataMapper sensorDataMapper;
    private final ExecutionLogMapper executionLogMapper;
    private final AutomationSettingMapper automationSettingMapper;
    private final AiReportMapper aiReportMapper;
    
    /**
     * 分析图片集并生成报告
     */
    @PostMapping("/analyze-images")
    public Result<String> analyzeImages(
            @RequestParam(value = "limit", defaultValue = "30") Integer limit,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        try {
            // 获取图片数据
            List<Image> images = imageMapper.selectList(null);
            
            // 根据日期范围过滤
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    Date startDateTime = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Date endDateTime = Date.from(end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    images = images.stream()
                        .filter(img -> {
                            if (img.getRecordTime() == null) return false;
                            return img.getRecordTime().after(startDateTime) && img.getRecordTime().before(endDateTime);
                        })
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    log.warn("日期解析失败，使用全部数据: {}", e.getMessage());
                }
            }
            
            // 排序
            images.sort((a, b) -> {
                if (a.getRecordTime() == null || b.getRecordTime() == null) return 0;
                return b.getRecordTime().compareTo(a.getRecordTime());
            });
            
            List<Image> recentImages = images.stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            // 转换为Map格式
            List<Map<String, Object>> imageData = recentImages.stream().map(img -> {
                Map<String, Object> map = new HashMap<>();
                map.put("recordTime", img.getRecordTime());
                map.put("temperatureC", img.getTemperatureC());
                map.put("humidityPct", img.getHumidityPct());
                map.put("soilMoisturePct", img.getSoilMoisturePct());
                map.put("lightLux", img.getLightLux());
                map.put("isAbnormal", img.getIsAbnormal());
                map.put("abnormalReason", img.getAbnormalReason());
                return map;
            }).collect(Collectors.toList());
            
            String report = aiService.analyzeImages(imageData);
            return Result.success(report);
            
        } catch (Exception e) {
            log.error("分析图片失败: {}", e.getMessage(), e);
            return Result.error("分析图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 分析传感器数据并生成报告
     */
    @PostMapping("/analyze-sensor-data")
    public Result<String> analyzeSensorData(
            @RequestParam(value = "limit", defaultValue = "30") Integer limit,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        try {
            // 获取传感器数据
            List<SensorData> sensorDataList = sensorDataMapper.selectList(null);
            
            // 根据日期范围过滤
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    Date startDateTime = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Date endDateTime = Date.from(end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    sensorDataList = sensorDataList.stream()
                        .filter(data -> {
                            if (data.getRecordTime() == null) return false;
                            return data.getRecordTime().after(startDateTime) && data.getRecordTime().before(endDateTime);
                        })
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    log.warn("日期解析失败，使用全部数据: {}", e.getMessage());
                }
            }
            
            // 排序
            sensorDataList.sort((a, b) -> {
                if (a.getRecordTime() == null || b.getRecordTime() == null) return 0;
                return b.getRecordTime().compareTo(a.getRecordTime());
            });
            
            List<SensorData> recentData = sensorDataList.stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            // 转换为Map格式
            List<Map<String, Object>> dataList = recentData.stream().map(data -> {
                Map<String, Object> map = new HashMap<>();
                map.put("recordTime", data.getRecordTime());
                map.put("temperatureC", data.getTemperatureC());
                map.put("humidityPct", data.getHumidityPct());
                map.put("soilMoisturePct", data.getSoilMoisturePct());
                map.put("lightLux", data.getLightLux());
                map.put("oxygenPct", data.getOxygenPct());
                map.put("co2Ppm", data.getCo2Ppm());
                return map;
            }).collect(Collectors.toList());
            
            String report = aiService.analyzeSensorData(dataList);
            return Result.success(report);
            
        } catch (Exception e) {
            log.error("分析传感器数据失败: {}", e.getMessage(), e);
            return Result.error("分析传感器数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成自动化控制建议
     */
    @PostMapping("/automation-advice")
    public Result<String> getAutomationAdvice() {
        try {
            // 获取最新传感器数据
            SensorData latest = sensorDataMapper.selectList(null).stream()
                .max(Comparator.comparing(SensorData::getRecordTime))
                .orElse(null);
            
            if (latest == null) {
                return Result.error("暂无传感器数据");
            }
            
            // 获取自动化设置（从key-value存储中读取）
            var automationSettings = automationSettingMapper.selectList(null);
            Map<String, Object> settings = new HashMap<>();
            for (var setting : automationSettings) {
                String key = setting.getSettingKey();
                String value = setting.getSettingValue();
                // 尝试转换为数字
                try {
                    if (value.contains(".")) {
                        settings.put(key, Double.parseDouble(value));
                    } else {
                        settings.put(key, Integer.parseInt(value));
                    }
                } catch (NumberFormatException e) {
                    settings.put(key, value);
                }
            }
            
            Map<String, Object> currentData = new HashMap<>();
            currentData.put("temperatureC", latest.getTemperatureC());
            currentData.put("humidityPct", latest.getHumidityPct());
            currentData.put("soilMoisturePct", latest.getSoilMoisturePct());
            currentData.put("lightLux", latest.getLightLux());
            currentData.put("oxygenPct", latest.getOxygenPct());
            currentData.put("co2Ppm", latest.getCo2Ppm());
            
            String advice = aiService.generateAutomationAdvice(currentData, settings);
            return Result.success(advice);
            
        } catch (Exception e) {
            log.error("获取自动化建议失败: {}", e.getMessage(), e);
            return Result.error("获取自动化建议失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成综合报告
     */
    @PostMapping("/comprehensive-report")
    public Result<String> generateComprehensiveReport(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        try {
            // 获取图片数据
            List<Image> images = imageMapper.selectList(null);
            
            // 根据日期范围过滤
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    Date startDateTime = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Date endDateTime = Date.from(end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    images = images.stream()
                        .filter(img -> {
                            if (img.getRecordTime() == null) return false;
                            return img.getRecordTime().after(startDateTime) && img.getRecordTime().before(endDateTime);
                        })
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    log.warn("日期解析失败，使用全部数据: {}", e.getMessage());
                }
            }
            
            images.sort((a, b) -> {
                if (a.getRecordTime() == null || b.getRecordTime() == null) return 0;
                return b.getRecordTime().compareTo(a.getRecordTime());
            });
            List<Map<String, Object>> imageData = images.stream().limit(10).map(img -> {
                Map<String, Object> map = new HashMap<>();
                map.put("recordTime", img.getRecordTime());
                map.put("temperatureC", img.getTemperatureC());
                map.put("humidityPct", img.getHumidityPct());
                map.put("soilMoisturePct", img.getSoilMoisturePct());
                map.put("isAbnormal", img.getIsAbnormal());
                return map;
            }).collect(Collectors.toList());
            
            // 获取传感器数据
            List<SensorData> sensorDataList = sensorDataMapper.selectList(null);
            
            // 根据日期范围过滤
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    Date startDateTime = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Date endDateTime = Date.from(end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    sensorDataList = sensorDataList.stream()
                        .filter(data -> {
                            if (data.getRecordTime() == null) return false;
                            return data.getRecordTime().after(startDateTime) && data.getRecordTime().before(endDateTime);
                        })
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    log.warn("日期解析失败，使用全部数据: {}", e.getMessage());
                }
            }
            
            sensorDataList.sort((a, b) -> {
                if (a.getRecordTime() == null || b.getRecordTime() == null) return 0;
                return b.getRecordTime().compareTo(a.getRecordTime());
            });
            List<Map<String, Object>> sensorData = sensorDataList.stream().limit(10).map(data -> {
                Map<String, Object> map = new HashMap<>();
                map.put("recordTime", data.getRecordTime());
                map.put("temperatureC", data.getTemperatureC());
                map.put("humidityPct", data.getHumidityPct());
                map.put("soilMoisturePct", data.getSoilMoisturePct());
                map.put("lightLux", data.getLightLux());
                return map;
            }).collect(Collectors.toList());
            
            // 获取执行日志
            var executionLogs = executionLogMapper.selectList(null);
            executionLogs.sort((a, b) -> {
                if (a.getExecutedAt() == null || b.getExecutedAt() == null) return 0;
                return b.getExecutedAt().compareTo(a.getExecutedAt());
            });
            List<Map<String, Object>> logData = executionLogs.stream().limit(10).map(log -> {
                Map<String, Object> map = new HashMap<>();
                map.put("executedAt", log.getExecutedAt());
                map.put("plotId", log.getPlotId());
                map.put("executions", log.getExecutions());
                return map;
            }).collect(Collectors.toList());
            
            // 获取自动化设置（从key-value存储中读取）
            var automationSettings = automationSettingMapper.selectList(null);
            Map<String, Object> settings = new HashMap<>();
            for (var setting : automationSettings) {
                String key = setting.getSettingKey();
                String value = setting.getSettingValue();
                // 尝试转换为数字
                try {
                    if (value.contains(".")) {
                        settings.put(key, Double.parseDouble(value));
                    } else {
                        settings.put(key, Integer.parseInt(value));
                    }
                } catch (NumberFormatException e) {
                    settings.put(key, value);
                }
            }
            
            String report = aiService.generateComprehensiveReport(imageData, sensorData, logData, settings);
            return Result.success(report);
            
        } catch (Exception e) {
            log.error("生成综合报告失败: {}", e.getMessage(), e);
            return Result.error("生成综合报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取AI自动执行建议
     */
    @PostMapping("/auto-execution-advice")
    public Result<Map<String, Object>> getAutoExecutionAdvice() {
        try {
            // 获取最新传感器数据
            SensorData latest = sensorDataMapper.selectList(null).stream()
                .max(Comparator.comparing(SensorData::getRecordTime))
                .orElse(null);
            
            if (latest == null) {
                return Result.error("暂无传感器数据");
            }
            
            Map<String, Object> currentData = new HashMap<>();
            currentData.put("temperatureC", latest.getTemperatureC());
            currentData.put("humidityPct", latest.getHumidityPct());
            currentData.put("soilMoisturePct", latest.getSoilMoisturePct());
            currentData.put("lightLux", latest.getLightLux());
            currentData.put("oxygenPct", latest.getOxygenPct());
            currentData.put("co2Ppm", latest.getCo2Ppm());
            
            Map<String, Object> advice = aiService.getAutoExecutionAdvice(currentData);
            return Result.success(advice);
            
        } catch (Exception e) {
            log.error("获取AI自动执行建议失败: {}", e.getMessage(), e);
            return Result.error("获取AI自动执行建议失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存AI分析报告
     */
    @PostMapping("/reports")
    @Transactional
    public Result<AiReport> saveReport(@RequestBody AiReportDTO dto) {
        try {
            AiReport report = new AiReport();
            report.setReportType(dto.getReportType());
            report.setReportTitle(dto.getReportTitle());
            report.setReportContent(dto.getReportContent());
            report.setDataCount(dto.getDataCount());
            
            // 解析日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) {
                report.setStartDate(sdf.parse(dto.getStartDate()));
            }
            if (dto.getEndDate() != null && !dto.getEndDate().isEmpty()) {
                report.setEndDate(sdf.parse(dto.getEndDate()));
            }
            
            report.setCreatedAt(new Date());
            report.setUpdatedAt(new Date());
            
            aiReportMapper.insert(report);
            return Result.success(report);
            
        } catch (Exception e) {
            log.error("保存AI报告失败: {}", e.getMessage(), e);
            return Result.error("保存AI报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有AI报告
     */
    @GetMapping("/reports")
    public Result<List<AiReport>> getAllReports(
            @RequestParam(value = "reportType", required = false) String reportType,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        try {
            List<AiReport> reports = aiReportMapper.selectList(null);
            
            // 按类型过滤
            if (reportType != null && !reportType.isEmpty()) {
                reports = reports.stream()
                    .filter(r -> reportType.equals(r.getReportType()))
                    .collect(Collectors.toList());
            }
            
            // 按日期范围过滤
            if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    Date startDateTime = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Date endDateTime = Date.from(end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    
                    reports = reports.stream()
                        .filter(r -> {
                            if (r.getCreatedAt() == null) return false;
                            return r.getCreatedAt().after(startDateTime) && r.getCreatedAt().before(endDateTime);
                        })
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    log.warn("日期解析失败: {}", e.getMessage());
                }
            }
            
            // 按创建时间倒序排序
            reports.sort((a, b) -> {
                if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
            
            return Result.success(reports);
            
        } catch (Exception e) {
            log.error("获取AI报告失败: {}", e.getMessage(), e);
            return Result.error("获取AI报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取AI报告
     */
    @GetMapping("/reports/{id}")
    public Result<AiReport> getReportById(@PathVariable Long id) {
        try {
            AiReport report = aiReportMapper.selectById(id);
            if (report == null) {
                return Result.error(404, "报告不存在");
            }
            return Result.success(report);
        } catch (Exception e) {
            log.error("获取AI报告失败: {}", e.getMessage(), e);
            return Result.error("获取AI报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除AI报告
     */
    @DeleteMapping("/reports/{id}")
    @Transactional
    public Result<Void> deleteReport(@PathVariable Long id) {
        try {
            aiReportMapper.deleteById(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除AI报告失败: {}", e.getMessage(), e);
            return Result.error("删除AI报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取自动报告开关状态
     */
    @GetMapping("/auto-report/enabled")
    public Result<Boolean> getAutoReportEnabled() {
        try {
            var setting = automationSettingMapper.findBySettingKey("ai_auto_report_enabled");
            if (setting == null) {
                return Result.success(false);
            }
            boolean enabled = "true".equalsIgnoreCase(setting.getSettingValue()) || "1".equals(setting.getSettingValue());
            return Result.success(enabled);
        } catch (Exception e) {
            log.error("获取自动报告开关失败: {}", e.getMessage(), e);
            return Result.error("获取自动报告开关失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置自动报告开关
     */
    @PutMapping("/auto-report/enabled")
    @Transactional
    public Result<Boolean> setAutoReportEnabled(@RequestParam Boolean enabled) {
        try {
            var setting = automationSettingMapper.findBySettingKey("ai_auto_report_enabled");
            if (setting == null) {
                setting = new com.greenhouse.entity.AutomationSetting();
                setting.setSettingKey("ai_auto_report_enabled");
                setting.setSettingValue(enabled ? "true" : "false");
                setting.setDescription("AI自动报告开关");
                setting.setCreatedAt(new Date());
                setting.setUpdatedAt(new Date());
                automationSettingMapper.insert(setting);
            } else {
                setting.setSettingValue(enabled ? "true" : "false");
                setting.setUpdatedAt(new Date());
                automationSettingMapper.updateById(setting);
            }
            log.info("自动报告开关已设置为: {}", enabled);
            return Result.success(enabled);
        } catch (Exception e) {
            log.error("设置自动报告开关失败: {}", e.getMessage(), e);
            return Result.error("设置自动报告开关失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动触发生成每日报告（用于测试）
     */
    @PostMapping("/auto-report/generate-now")
    public Result<String> generateReportNow() {
        try {
            // 这里可以调用定时任务的方法，或者直接生成报告
            // 为了简化，我们直接生成昨天的报告
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String startDate = yesterday.toString();
            String endDate = yesterday.toString();
            
            String report = aiService.generateComprehensiveReport(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap()
            );
            
            // 如果启用了自动保存，则保存报告
            var setting = automationSettingMapper.findBySettingKey("ai_auto_report_enabled");
            if (setting != null && ("true".equalsIgnoreCase(setting.getSettingValue()) || "1".equals(setting.getSettingValue()))) {
                AiReport aiReport = new AiReport();
                aiReport.setReportType("comprehensive_report");
                aiReport.setReportTitle(String.format("手动触发报告 - %s", yesterday.toString()));
                aiReport.setReportContent(report);
                aiReport.setStartDate(Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                aiReport.setEndDate(Date.from(yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                aiReport.setDataCount(0);
                aiReport.setCreatedAt(new Date());
                aiReport.setUpdatedAt(new Date());
                aiReportMapper.insert(aiReport);
                return Result.success("报告已生成并保存");
            }
            
            return Result.success("报告已生成（未保存，自动保存功能未启用）");
        } catch (Exception e) {
            log.error("手动生成报告失败: {}", e.getMessage(), e);
            return Result.error("手动生成报告失败: " + e.getMessage());
        }
    }
}

