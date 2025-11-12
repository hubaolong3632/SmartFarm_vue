package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.entity.ExecutionLog;
import com.greenhouse.mapper.ExecutionLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 执行日志控制器
 */
@RestController
@RequestMapping("/execution-logs")
@RequiredArgsConstructor
public class ExecutionLogController {
    
    private final ExecutionLogMapper executionLogMapper;
    
    /**
     * 获取所有执行日志
     */
    @GetMapping
    public Result<List<ExecutionLog>> getAll() {
        return Result.success(executionLogMapper.selectList(null));
    }
    
    /**
     * 获取指定地块的执行日志
     */
    @GetMapping("/plot")
    public Result<List<ExecutionLog>> getByPlotId(@RequestParam Integer plotId) {
        List<ExecutionLog> logs = executionLogMapper.findByPlotIdOrderByExecutedAtDesc(plotId);
        return Result.success(logs);
    }
    
    /**
     * 获取最近24小时的执行统计（按小时聚合）
     */
    @GetMapping("/last-24-hours")
    public Result<List<Map<String, Object>>> getLast24Hours() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(23);
        
        List<Object[]> results = executionLogMapper.findExecutionsByHour(startTime, endTime);
        List<Map<String, Object>> data = results.stream().map(row -> {
            return Map.of(
                    "time", row[0],
                    "count", row[1]
            );
        }).collect(Collectors.toList());
        
        return Result.success(data);
    }
    
    /**
     * 获取指定时间范围的执行日志
     */
    @GetMapping("/range")
    public Result<List<ExecutionLog>> getByTimeRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<ExecutionLog> logs = executionLogMapper.findByExecutedAtBetweenOrderByExecutedAtDesc(startTime, endTime);
        return Result.success(logs);
    }
}
