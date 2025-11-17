package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.AiReportDTO;
import com.greenhouse.entity.AiReport;
import com.greenhouse.entity.AiExecutionLog;
import com.greenhouse.entity.Image;
import com.greenhouse.entity.Plot;
import com.greenhouse.entity.Recipe;
import com.greenhouse.entity.SensorData;
import com.greenhouse.mapper.AiReportMapper;
import com.greenhouse.mapper.AiExecutionLogMapper;
import com.greenhouse.mapper.ImageMapper;
import com.greenhouse.mapper.SensorDataMapper;
import com.greenhouse.mapper.ExecutionLogMapper;
import com.greenhouse.mapper.AutomationSettingMapper;
import com.greenhouse.mapper.PlotMapper;
import com.greenhouse.mapper.RecipeMapper;
import com.greenhouse.service.AiService;
import com.greenhouse.service.MqttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private final AiExecutionLogMapper aiExecutionLogMapper;
    private final PlotMapper plotMapper;
    private final RecipeMapper recipeMapper;
    private final MqttService mqttService;
    
    /**
     * 分析图片集并生成报告（流式）
     */
    @GetMapping(value = "/analyze-images-stream", produces = "text/event-stream")
    public SseEmitter analyzeImagesStream(
            @RequestParam(value = "limit", defaultValue = "30") Integer limit,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        CompletableFuture.runAsync(() -> {
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
                
                emitter.send(SseEmitter.event().name("start").data("{\"status\":\"start\"}"));
                
                aiService.analyzeImagesStream(imageData,
                    chunk -> {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data("{\"content\":\"" + 
                                    chunk.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}"));
                        } catch (Exception e) {
                            log.error("发送流数据失败", e);
                        }
                    },
                    () -> {
                        try {
                            emitter.send(SseEmitter.event().name("complete").data("{\"status\":\"complete\"}"));
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    });
                
            } catch (Exception e) {
                log.error("分析图片失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + e.getMessage() + "\"}"));
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        return emitter;
    }
    
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
     * 分析传感器数据并生成报告（流式）
     */
    @GetMapping(value = "/analyze-sensor-data-stream", produces = "text/event-stream")
    public SseEmitter analyzeSensorDataStream(
            @RequestParam(value = "limit", defaultValue = "30") Integer limit,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        SseEmitter emitter = new SseEmitter(300000L);
        
        CompletableFuture.runAsync(() -> {
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
                
                emitter.send(SseEmitter.event().name("start").data("{\"status\":\"start\"}"));
                
                aiService.analyzeSensorDataStream(dataList,
                    chunk -> {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data("{\"content\":\"" + 
                                    chunk.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}"));
                        } catch (Exception e) {
                            log.error("发送流数据失败", e);
                        }
                    },
                    () -> {
                        try {
                            emitter.send(SseEmitter.event().name("complete").data("{\"status\":\"complete\"}"));
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    });
                
            } catch (Exception e) {
                log.error("分析传感器数据失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + e.getMessage() + "\"}"));
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        return emitter;
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
     * 生成自动化控制建议（流式）
     */
    @GetMapping(value = "/automation-advice-stream", produces = "text/event-stream")
    public SseEmitter getAutomationAdviceStream() {
        SseEmitter emitter = new SseEmitter(300000L);
        
        CompletableFuture.runAsync(() -> {
            try {
                // 获取最新传感器数据
                SensorData latest = sensorDataMapper.selectList(null).stream()
                    .max(Comparator.comparing(SensorData::getRecordTime))
                    .orElse(null);
                
                if (latest == null) {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"暂无传感器数据\"}"));
                    emitter.complete();
                    return;
                }
                
                // 获取自动化设置
                var automationSettings = automationSettingMapper.selectList(null);
                Map<String, Object> settings = new HashMap<>();
                for (var setting : automationSettings) {
                    String key = setting.getSettingKey();
                    String value = setting.getSettingValue();
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
                
                emitter.send(SseEmitter.event().name("start").data("{\"status\":\"start\"}"));
                
                aiService.generateAutomationAdviceStream(currentData, settings,
                    chunk -> {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data("{\"content\":\"" + 
                                    chunk.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}"));
                        } catch (Exception e) {
                            log.error("发送流数据失败", e);
                        }
                    },
                    () -> {
                        try {
                            emitter.send(SseEmitter.event().name("complete").data("{\"status\":\"complete\"}"));
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    });
                
            } catch (Exception e) {
                log.error("获取自动化建议失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + e.getMessage() + "\"}"));
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        return emitter;
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
     * 生成综合报告（流式）
     */
    @GetMapping(value = "/comprehensive-report-stream", produces = "text/event-stream")
    public SseEmitter generateComprehensiveReportStream(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        SseEmitter emitter = new SseEmitter(300000L);
        
        CompletableFuture.runAsync(() -> {
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
                
                // 获取自动化设置
                var automationSettings = automationSettingMapper.selectList(null);
                Map<String, Object> settings = new HashMap<>();
                for (var setting : automationSettings) {
                    String key = setting.getSettingKey();
                    String value = setting.getSettingValue();
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
                
                emitter.send(SseEmitter.event().name("start").data("{\"status\":\"start\"}"));
                
                aiService.generateComprehensiveReportStream(imageData, sensorData, logData, settings,
                    chunk -> {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data("{\"content\":\"" + 
                                    chunk.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}"));
                        } catch (Exception e) {
                            log.error("发送流数据失败", e);
                        }
                    },
                    () -> {
                        try {
                            emitter.send(SseEmitter.event().name("complete").data("{\"status\":\"complete\"}"));
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    });
                
            } catch (Exception e) {
                log.error("生成综合报告失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + e.getMessage() + "\"}"));
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        return emitter;
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
     * 获取AI自动执行建议（流式）
     */
    @GetMapping(value = "/auto-execution-advice-stream", produces = "text/event-stream")
    public SseEmitter getAutoExecutionAdviceStream() {
        SseEmitter emitter = new SseEmitter(300000L);
        
        CompletableFuture.runAsync(() -> {
            try {
                // 获取最新传感器数据
                SensorData latest = sensorDataMapper.selectList(null).stream()
                    .max(Comparator.comparing(SensorData::getRecordTime))
                    .orElse(null);
                
                if (latest == null) {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"暂无传感器数据\"}"));
                    emitter.complete();
                    return;
                }
                
                Map<String, Object> currentData = new HashMap<>();
                currentData.put("temperatureC", latest.getTemperatureC());
                currentData.put("humidityPct", latest.getHumidityPct());
                currentData.put("soilMoisturePct", latest.getSoilMoisturePct());
                currentData.put("lightLux", latest.getLightLux());
                currentData.put("oxygenPct", latest.getOxygenPct());
                currentData.put("co2Ppm", latest.getCo2Ppm());
                
                emitter.send(SseEmitter.event().name("start").data("{\"status\":\"start\"}"));
                
                // 对于auto-execution-advice，我们需要流式返回summary部分
                StringBuilder summary = new StringBuilder();
                aiService.callAiApiStream(
                    "基于当前温室数据，请提供自动化执行建议。\n\n" +
                    "当前数据：\n" +
                    String.format("温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 光照: %s lux, 氧气: %s%%, 二氧化碳: %s ppm\n",
                        currentData.get("temperatureC"), currentData.get("humidityPct"),
                        currentData.get("soilMoisturePct"), currentData.get("lightLux"),
                        currentData.get("oxygenPct"), currentData.get("co2Ppm")) +
                    "\n请以JSON格式返回建议，格式如下：\n" +
                    "{\n" +
                    "  \"actions\": [\n" +
                    "    {\"type\": \"light\", \"action\": \"on/off\", \"reason\": \"原因\"},\n" +
                    "    {\"type\": \"pump\", \"action\": \"on/off\", \"reason\": \"原因\"},\n" +
                    "    {\"type\": \"recipe\", \"plotId\": 1, \"recipeId\": \"配方ID\", \"executions\": 1, \"reason\": \"原因\"}\n" +
                    "  ],\n" +
                    "  \"summary\": \"执行建议总结\"\n" +
                    "}\n",
                    chunk -> {
                        summary.append(chunk);
                        try {
                            emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data("{\"content\":\"" + 
                                    chunk.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}"));
                        } catch (Exception e) {
                            log.error("发送流数据失败", e);
                        }
                    },
                    () -> {
                        try {
                            // 尝试解析完整的响应为JSON
                            String fullResponse = summary.toString();
                            try {
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                String jsonStr = fullResponse;
                                if (fullResponse.contains("```json")) {
                                    int start = fullResponse.indexOf("```json") + 7;
                                    int end = fullResponse.indexOf("```", start);
                                    jsonStr = fullResponse.substring(start, end).trim();
                                } else if (fullResponse.contains("```")) {
                                    int start = fullResponse.indexOf("```") + 3;
                                    int end = fullResponse.indexOf("```", start);
                                    jsonStr = fullResponse.substring(start, end).trim();
                                }
                                Map<String, Object> result = mapper.readValue(jsonStr, Map.class);
                                emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data("{\"status\":\"complete\",\"data\":" + mapper.writeValueAsString(result) + "}"));
                            } catch (Exception e) {
                                // 如果解析失败，只返回summary
                                Map<String, Object> result = new HashMap<>();
                                result.put("summary", fullResponse);
                                result.put("actions", new ArrayList<>());
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data("{\"status\":\"complete\",\"data\":" + mapper.writeValueAsString(result) + "}"));
                            }
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    });
                
            } catch (Exception e) {
                log.error("获取AI自动执行建议失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + e.getMessage() + "\"}"));
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 执行AI自动执行建议的操作（推送到MQTT）
     */
    @PostMapping("/auto-execution-advice/execute")
    public Result<String> executeAiAction(@RequestBody Map<String, Object> action) {
        try {
            Integer operationType = parseInteger(action.get("type"));
            if (operationType == null) {
                String typeStr = action.get("type") != null ? action.get("type").toString() : null;
                if ("light".equalsIgnoreCase(typeStr)) {
                    operationType = 1;
                } else if ("pump".equalsIgnoreCase(typeStr) || "water".equalsIgnoreCase(typeStr)) {
                    operationType = 2;
                } else if ("recipe".equalsIgnoreCase(typeStr) || "nutrient".equalsIgnoreCase(typeStr)) {
                    operationType = 3;
                }
            }
            if (operationType == null) {
                return Result.error("操作类型无效");
            }

            Integer plotId = parseInteger(action.get("plotId"));
            if (plotId == null) {
                return Result.error("缺少 plotId");
            }

            Integer executions = parseInteger(action.get("executions"));
            if (executions == null || executions <= 0) {
                executions = 1;
            }

            String recipeId = action.get("recipeId") != null ? action.get("recipeId").toString() : null;

            Map<String, Object> message = new HashMap<>();
            message.put("operationType", operationType);
            message.put("plotId", plotId);
            message.put("reason", action.get("reason"));
            message.put("executeTime", System.currentTimeMillis());
            message.put("source", "ai_auto_execution");

            if (operationType == 1) {
                message.put("operation", "light");
                message.put("action", action.getOrDefault("action", "on"));
            } else if (operationType == 2) {
                message.put("operation", "water");
                message.put("action", action.getOrDefault("action", "start"));
            } else if (operationType == 3) {
                if (recipeId == null) {
                    return Result.error("类型3需要提供 recipeId");
                }
                Plot plot = plotMapper.selectById(plotId);
                if (plot == null) {
                    return Result.error("地块不存在");
                }
                Recipe recipe = recipeMapper.selectById(recipeId);
                if (recipe == null) {
                    return Result.error("配方不存在");
                }
                message.put("plotName", plot.getName() != null ? plot.getName() : "地块" + plot.getPlotNumber());
                message.put("recipeId", recipeId);
                message.put("recipeName", recipe.getName());
                message.put("waterMl", recipe.getWaterMl() != null ? recipe.getWaterMl() : 0);
                message.put("nutrientMl", recipe.getNutrientMl() != null ? recipe.getNutrientMl() : 0);
                message.put("rootingPowderMl", recipe.getRootingPowderMl() != null ? recipe.getRootingPowderMl() : 0);
                message.put("specialMl", recipe.getSpecialMl() != null ? recipe.getSpecialMl() : 0);
                message.put("executions", executions);
            } else {
                return Result.error("未知的操作类型");
            }

            // 转换为JSON字符串
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonMessage = mapper.writeValueAsString(message);
            
            // 发送到MQTT主题 "voluntarily"
            mqttService.publish("voluntarily", jsonMessage, 1);
            log.info("已发送AI自动执行建议到MQTT主题 'voluntarily': {}", jsonMessage);

            // 保存执行记录到数据库
            AiExecutionLog logEntity = new AiExecutionLog();
            logEntity.setOperationType(String.valueOf(operationType));
            Object actionObj = action.get("action");
            logEntity.setAction(actionObj != null ? actionObj.toString() : null);
            logEntity.setPlotId(plotId);
            if (recipeId != null) {
                logEntity.setRecipeId(recipeId);
            }
            logEntity.setExecutions(executions);
            Object reasonObj = action.get("reason");
            if (reasonObj != null) {
                logEntity.setReason(reasonObj.toString());
            }
            logEntity.setPayload(jsonMessage);
            logEntity.setExecuteTime(new Date());
            logEntity.setCreatedAt(new Date());
            aiExecutionLogMapper.insert(logEntity);
            
            return Result.success("操作已发送到MQTT");
            
        } catch (Exception e) {
            log.error("执行AI自动执行建议失败: {}", e.getMessage(), e);
            return Result.error("执行失败: " + e.getMessage());
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
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
    
    /**
     * 一键分析（流式输出所有分析结果）
     */
    @GetMapping(value = "/analyze-all-stream", produces = "text/event-stream")
    public SseEmitter analyzeAllStream(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        CompletableFuture.runAsync(() -> {
            try {
                // 获取数据
                List<Image> images = imageMapper.selectList(null);
                List<SensorData> sensorDataList = sensorDataMapper.selectList(null);
                List<com.greenhouse.entity.ExecutionLog> executionLogs = executionLogMapper.selectList(null);
                
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
                
                // 转换为Map格式
                List<Map<String, Object>> imageData = images.stream()
                    .sorted((a, b) -> {
                        if (a.getRecordTime() == null || b.getRecordTime() == null) return 0;
                        return b.getRecordTime().compareTo(a.getRecordTime());
                    })
                    .limit(30)
                    .map(img -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("recordTime", img.getRecordTime());
                        map.put("temperatureC", img.getTemperatureC());
                        map.put("humidityPct", img.getHumidityPct());
                        map.put("soilMoisturePct", img.getSoilMoisturePct());
                        map.put("lightLux", img.getLightLux());
                        map.put("isAbnormal", img.getIsAbnormal());
                        return map;
                    }).collect(Collectors.toList());
                
                List<Map<String, Object>> sensorData = sensorDataList.stream()
                    .sorted((a, b) -> {
                        if (a.getRecordTime() == null || b.getRecordTime() == null) return 0;
                        return b.getRecordTime().compareTo(a.getRecordTime());
                    })
                    .limit(30)
                    .map(data -> {
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
                
                List<Map<String, Object>> logData = executionLogs.stream()
                    .sorted((a, b) -> {
                        if (a.getExecutedAt() == null || b.getExecutedAt() == null) return 0;
                        return b.getExecutedAt().compareTo(a.getExecutedAt());
                    })
                    .limit(10)
                    .map(log -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("executedAt", log.getExecutedAt());
                        map.put("plotId", log.getPlotId());
                        map.put("executions", log.getExecutions());
                        return map;
                    }).collect(Collectors.toList());
                
                // 获取自动化设置
                Map<String, Object> automationSettings = new HashMap<>();
                var settings = automationSettingMapper.selectList(null);
                for (var setting : settings) {
                    automationSettings.put(setting.getSettingKey(), setting.getSettingValue());
                }
                
                // 获取最新传感器数据
                SensorData latest = sensorDataMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SensorData>()
                        .orderByDesc("record_time")
                        .last("LIMIT 1")
                );
                Map<String, Object> currentData = new HashMap<>();
                if (latest != null) {
                    currentData.put("temperatureC", latest.getTemperatureC());
                    currentData.put("humidityPct", latest.getHumidityPct());
                    currentData.put("soilMoisturePct", latest.getSoilMoisturePct());
                    currentData.put("lightLux", latest.getLightLux());
                    currentData.put("oxygenPct", latest.getOxygenPct());
                    currentData.put("co2Ppm", latest.getCo2Ppm());
                }
                
                // 使用原子计数器跟踪完成的任务数
                java.util.concurrent.atomic.AtomicInteger completedTasks = new java.util.concurrent.atomic.AtomicInteger(0);
                final int totalTasks = 4;
                
                // 1. 图片分析（流式）- 并行执行
                CompletableFuture.runAsync(() -> {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("start")
                            .data("{\"type\":\"image_analysis\",\"status\":\"start\"}"));
                        
                        StringBuilder imageReport = new StringBuilder();
                        aiService.analyzeImagesStream(imageData, 
                            chunk -> {
                                imageReport.append(chunk);
                                try {
                                    emitter.send(SseEmitter.event()
                                        .name("chunk")
                                        .data("{\"type\":\"image_analysis\",\"content\":\"" + 
                                            chunk.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}"));
                                } catch (Exception e) {
                                    log.error("发送图片分析流数据失败", e);
                                }
                            },
                            () -> {
                                try {
                                    emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("{\"type\":\"image_analysis\",\"status\":\"complete\"}"));
                                    
                                    // 检查是否所有任务完成
                                    if (completedTasks.incrementAndGet() == totalTasks) {
                                        emitter.send(SseEmitter.event()
                                            .name("all_complete")
                                            .data("{\"status\":\"all_complete\"}"));
                                        emitter.complete();
                                    }
                                } catch (Exception e) {
                                    log.error("发送图片分析完成事件失败", e);
                                }
                            });
                    } catch (Exception e) {
                        log.error("图片分析失败", e);
                        if (completedTasks.incrementAndGet() == totalTasks) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("all_complete")
                                    .data("{\"status\":\"all_complete\"}"));
                                emitter.complete();
                            } catch (Exception ex) {
                                emitter.completeWithError(ex);
                            }
                        }
                    }
                });
                
                // 2. 传感器数据分析（流式）- 并行执行
                CompletableFuture.runAsync(() -> {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("start")
                            .data("{\"type\":\"sensor_analysis\",\"status\":\"start\"}"));
                        
                        StringBuilder sensorReport = new StringBuilder();
                        aiService.analyzeSensorDataStream(sensorData,
                            chunk -> {
                                sensorReport.append(chunk);
                                try {
                                    emitter.send(SseEmitter.event()
                                        .name("chunk")
                                        .data("{\"type\":\"sensor_analysis\",\"content\":\"" + 
                                            chunk.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}"));
                                } catch (Exception e) {
                                    log.error("发送传感器分析流数据失败", e);
                                }
                            },
                            () -> {
                                try {
                                    emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("{\"type\":\"sensor_analysis\",\"status\":\"complete\"}"));
                                    
                                    // 检查是否所有任务完成
                                    if (completedTasks.incrementAndGet() == totalTasks) {
                                        emitter.send(SseEmitter.event()
                                            .name("all_complete")
                                            .data("{\"status\":\"all_complete\"}"));
                                        emitter.complete();
                                    }
                                } catch (Exception e) {
                                    log.error("发送传感器分析完成事件失败", e);
                                }
                            });
                    } catch (Exception e) {
                        log.error("传感器分析失败", e);
                        if (completedTasks.incrementAndGet() == totalTasks) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("all_complete")
                                    .data("{\"status\":\"all_complete\"}"));
                                emitter.complete();
                            } catch (Exception ex) {
                                emitter.completeWithError(ex);
                            }
                        }
                    }
                });
                
                // 3. 自动化建议（流式）- 并行执行
                CompletableFuture.runAsync(() -> {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("start")
                            .data("{\"type\":\"automation_advice\",\"status\":\"start\"}"));
                        
                        StringBuilder automationAdvice = new StringBuilder();
                        aiService.generateAutomationAdviceStream(currentData, automationSettings,
                            chunk -> {
                                automationAdvice.append(chunk);
                                try {
                                    emitter.send(SseEmitter.event()
                                        .name("chunk")
                                        .data("{\"type\":\"automation_advice\",\"content\":\"" + 
                                            chunk.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}"));
                                } catch (Exception e) {
                                    log.error("发送自动化建议流数据失败", e);
                                }
                            },
                            () -> {
                                try {
                                    emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("{\"type\":\"automation_advice\",\"status\":\"complete\"}"));
                                    
                                    // 检查是否所有任务完成
                                    if (completedTasks.incrementAndGet() == totalTasks) {
                                        emitter.send(SseEmitter.event()
                                            .name("all_complete")
                                            .data("{\"status\":\"all_complete\"}"));
                                        emitter.complete();
                                    }
                                } catch (Exception e) {
                                    log.error("发送自动化建议完成事件失败", e);
                                }
                            });
                    } catch (Exception e) {
                        log.error("自动化建议失败", e);
                        if (completedTasks.incrementAndGet() == totalTasks) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("all_complete")
                                    .data("{\"status\":\"all_complete\"}"));
                                emitter.complete();
                            } catch (Exception ex) {
                                emitter.completeWithError(ex);
                            }
                        }
                    }
                });
                
                // 4. 综合报告（流式）- 并行执行
                CompletableFuture.runAsync(() -> {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("start")
                            .data("{\"type\":\"comprehensive_report\",\"status\":\"start\"}"));
                        
                        StringBuilder comprehensiveReport = new StringBuilder();
                        aiService.generateComprehensiveReportStream(imageData, sensorData, logData, automationSettings,
                            chunk -> {
                                comprehensiveReport.append(chunk);
                                try {
                                    emitter.send(SseEmitter.event()
                                        .name("chunk")
                                        .data("{\"type\":\"comprehensive_report\",\"content\":\"" + 
                                            chunk.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}"));
                                } catch (Exception e) {
                                    log.error("发送综合报告流数据失败", e);
                                }
                            },
                            () -> {
                                try {
                                    emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("{\"type\":\"comprehensive_report\",\"status\":\"complete\"}"));
                                    
                                    // 检查是否所有任务完成
                                    if (completedTasks.incrementAndGet() == totalTasks) {
                                        emitter.send(SseEmitter.event()
                                            .name("all_complete")
                                            .data("{\"status\":\"all_complete\"}"));
                                        emitter.complete();
                                    }
                                } catch (Exception e) {
                                    log.error("发送综合报告完成事件失败", e);
                                }
                            });
                    } catch (Exception e) {
                        log.error("综合报告失败", e);
                        if (completedTasks.incrementAndGet() == totalTasks) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("all_complete")
                                    .data("{\"status\":\"all_complete\"}"));
                                emitter.complete();
                            } catch (Exception ex) {
                                emitter.completeWithError(ex);
                            }
                        }
                    }
                });
                
            } catch (Exception e) {
                log.error("一键分析失败", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"" + e.getMessage() + "\"}"));
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        
        return emitter;
    }
}

