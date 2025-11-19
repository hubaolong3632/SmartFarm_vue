package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.entity.AiHostingConfig;
import com.greenhouse.entity.AiHostingLog;
import com.greenhouse.mapper.AiHostingConfigMapper;
import com.greenhouse.mapper.AiHostingLogMapper;
import com.greenhouse.service.AiHostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI托管控制器
 */
@RestController
@RequestMapping("/ai-hosting")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AiHostingController {
    
    private final AiHostingConfigMapper configMapper;
    private final AiHostingLogMapper logMapper;
    private final AiHostingService aiHostingService;
    
    /**
     * 获取AI托管配置
     */
    @GetMapping("/config")
    public Result<AiHostingConfig> getConfig() {
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
            return Result.success(config);
        }
        return Result.success(configs.get(0));
    }
    
    /**
     * 更新AI托管配置
     */
    @PutMapping("/config")
    @Transactional
    public Result<AiHostingConfig> updateConfig(@RequestBody AiHostingConfig config) {
        List<AiHostingConfig> configs = configMapper.selectList(null);
        if (configs.isEmpty()) {
            config.setCreatedAt(new Date());
            config.setUpdatedAt(new Date());
            configMapper.insert(config);
        } else {
            AiHostingConfig existing = configs.get(0);
            existing.setEnabled(config.getEnabled());
            existing.setEmailEnabled(config.getEmailEnabled());
            existing.setEmailAddress(config.getEmailAddress());
            existing.setCheckIntervalMinutes(config.getCheckIntervalMinutes());
            existing.setWaterControlEnabled(config.getWaterControlEnabled());
            existing.setLightControlEnabled(config.getLightControlEnabled());
            existing.setRecipeExecutionEnabled(config.getRecipeExecutionEnabled());
            existing.setUpdatedAt(new Date());
            configMapper.updateById(existing);
            config = existing;
        }
        
        // 重新调度定时任务（如果检查间隔改变了）
        aiHostingService.rescheduleTask();
        
        return Result.success(config);
    }
    
    /**
     * 获取执行日志
     */
    @GetMapping("/logs")
    public Result<List<AiHostingLog>> getLogs(
            @RequestParam(value = "limit", defaultValue = "50") Integer limit) {
        List<AiHostingLog> logs = logMapper.findRecentLogs(limit);
        return Result.success(logs);
    }
    
    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        List<AiHostingLog> allLogs = logMapper.selectList(null);
        
        long totalExecutions = allLogs.size();
        long successCount = allLogs.stream()
            .filter(log -> "success".equals(log.getStatus()))
            .count();
        long failedCount = allLogs.stream()
            .filter(log -> "failed".equals(log.getStatus()))
            .count();
        long partialCount = allLogs.stream()
            .filter(log -> "partial".equals(log.getStatus()))
            .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExecutions", totalExecutions);
        stats.put("successCount", successCount);
        stats.put("failedCount", failedCount);
        stats.put("partialCount", partialCount);
        stats.put("successRate", totalExecutions > 0 ? (double) successCount / totalExecutions * 100 : 0);
        
        return Result.success(stats);
    }
}

