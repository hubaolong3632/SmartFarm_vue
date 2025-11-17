package com.greenhouse.service;

import com.greenhouse.dto.AiReportDTO;
import com.greenhouse.entity.AiReport;
import com.greenhouse.entity.Image;
import com.greenhouse.entity.SensorData;
import com.greenhouse.mapper.AiReportMapper;
import com.greenhouse.mapper.AutomationSettingMapper;
import com.greenhouse.mapper.ExecutionLogMapper;
import com.greenhouse.mapper.ImageMapper;
import com.greenhouse.mapper.SensorDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI报告定时任务服务
 * 每天自动生成综合报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportScheduleService {
    
    private final AiService aiService;
    private final AiReportMapper aiReportMapper;
    private final ImageMapper imageMapper;
    private final SensorDataMapper sensorDataMapper;
    private final ExecutionLogMapper executionLogMapper;
    private final AutomationSettingMapper automationSettingMapper;
    
    /**
     * 检查是否启用自动报告
     */
    private boolean isAutoReportEnabled() {
        try {
            var setting = automationSettingMapper.findBySettingKey("ai_auto_report_enabled");
            if (setting == null) {
                return false; // 默认关闭
            }
            return "true".equalsIgnoreCase(setting.getSettingValue()) || "1".equals(setting.getSettingValue());
        } catch (Exception e) {
            log.warn("检查自动报告开关失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 每天凌晨2点自动生成综合报告
     * cron表达式: 秒 分 时 日 月 周
     * 0 0 2 * * ? 表示每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void generateDailyReport() {
        if (!isAutoReportEnabled()) {
            log.info("自动报告功能未启用，跳过生成");
            return;
        }
        
        log.info("开始执行每日自动生成AI报告任务");
        
        try {
            // 获取昨天的日期范围
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String startDate = yesterday.toString();
            String endDate = yesterday.toString();
            
            // 获取昨天的图片数据
            List<Image> images = imageMapper.selectList(null);
            Date startDateTime = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endDateTime = Date.from(yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            
            images = images.stream()
                .filter(img -> {
                    if (img.getRecordTime() == null) return false;
                    return img.getRecordTime().after(startDateTime) && img.getRecordTime().before(endDateTime);
                })
                .collect(Collectors.toList());
            
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
            
            // 获取昨天的传感器数据
            List<SensorData> sensorDataList = sensorDataMapper.selectList(null);
            sensorDataList = sensorDataList.stream()
                .filter(data -> {
                    if (data.getRecordTime() == null) return false;
                    return data.getRecordTime().after(startDateTime) && data.getRecordTime().before(endDateTime);
                })
                .collect(Collectors.toList());
            
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
            Map<String, Object> settingsMap = new HashMap<>();
            for (var setting : automationSettings) {
                String key = setting.getSettingKey();
                String value = setting.getSettingValue();
                try {
                    if (value.contains(".")) {
                        settingsMap.put(key, Double.parseDouble(value));
                    } else {
                        settingsMap.put(key, Integer.parseInt(value));
                    }
                } catch (NumberFormatException e) {
                    settingsMap.put(key, value);
                }
            }
            
            // 生成报告
            String reportContent = aiService.generateComprehensiveReport(imageData, sensorData, logData, settingsMap);
            
            // 保存报告
            AiReport report = new AiReport();
            report.setReportType("comprehensive_report");
            report.setReportTitle(String.format("每日自动报告 - %s", yesterday.toString()));
            report.setReportContent(reportContent);
            report.setStartDate(Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            report.setEndDate(Date.from(yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            report.setDataCount(imageData.size() + sensorData.size());
            report.setCreatedAt(new Date());
            report.setUpdatedAt(new Date());
            
            aiReportMapper.insert(report);
            
            log.info("每日自动报告生成成功，报告ID: {}, 日期: {}", report.getId(), yesterday.toString());
            
        } catch (Exception e) {
            log.error("每日自动生成报告失败: {}", e.getMessage(), e);
        }
    }
}

