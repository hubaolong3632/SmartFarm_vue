package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.entity.ExecutionLog;
import com.greenhouse.mapper.ExecutionLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 执行日志控制器
 */
@Slf4j
@RestController
@RequestMapping("/execution-logs")
@RequiredArgsConstructor
@CrossOrigin("*")
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
        try {
            Date endTime = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(endTime);
            cal.add(Calendar.HOUR_OF_DAY, -23);
            Date startTime = cal.getTime();
            
            log.debug("查询执行日志统计，时间范围: {} 到 {}", startTime, endTime);
            
            List<Map<String, Object>> results = executionLogMapper.findExecutionsByHour(startTime, endTime);
            log.debug("查询结果数量: {}", results != null ? results.size() : 0);
            
            if (results == null || results.isEmpty()) {
                log.debug("没有执行日志数据，返回空列表");
                return Result.success(new ArrayList<>());
            }
            
            List<Map<String, Object>> data = new ArrayList<>();
            for (Map<String, Object> row : results) {
                try {
                    // 从 Map 中获取数据
                    Object timeObj = row.get("hour");
                    Object countObj = row.get("totalExecutions");
                    
                    String timeStr = "";
                    if (timeObj != null) {
                        timeStr = timeObj.toString();
                    }
                    
                    long countValue = 0;
                    if (countObj != null) {
                        if (countObj instanceof Number) {
                            countValue = ((Number) countObj).longValue();
                        } else {
                            try {
                                countValue = Long.parseLong(countObj.toString());
                            } catch (NumberFormatException e) {
                                log.warn("无法解析执行次数: {}", countObj);
                                countValue = 0;
                            }
                        }
                    }
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("time", timeStr);
                    item.put("count", countValue);
                    data.add(item);
                    
                } catch (Exception e) {
                    log.error("处理执行日志数据行时出错: {}", e.getMessage(), e);
                    // 跳过这一行，继续处理下一行
                }
            }
            
            log.debug("成功处理 {} 条执行日志统计", data.size());
            return Result.success(data);
            
        } catch (Exception e) {
            log.error("获取执行日志统计失败: {}", e.getMessage(), e);
            // 如果查询失败，返回空列表而不是抛出异常
            return Result.success(new ArrayList<>());
        }
    }
    
    /**
     * 获取指定时间范围的执行日志
     */
    @GetMapping("/range")
    public Result<List<ExecutionLog>> getByTimeRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        List<ExecutionLog> logs = executionLogMapper.findByExecutedAtBetweenOrderByExecutedAtDesc(startTime, endTime);
        return Result.success(logs);
    }
}
