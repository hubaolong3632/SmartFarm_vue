package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.PlotAssignmentDTO;
import com.greenhouse.dto.PlotScheduleDTO;
import com.greenhouse.entity.*;
import com.greenhouse.mapper.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地块管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/plots")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PlotController {
    
    private final PlotMapper plotMapper;
    private final PlotAssignmentMapper plotAssignmentMapper;
    private final PlotScheduleMapper plotScheduleMapper;
    private final RecipeMapper recipeMapper;
    private final ExecutionLogMapper executionLogMapper;
    private final com.greenhouse.service.ScheduleTaskService scheduleTaskService;
    
    /**
     * 获取所有地块
     */
    @GetMapping
    public Result<List<Plot>> getAll() {
        return Result.success(plotMapper.selectList(null));
    }
    
    /**
     * 分配配方到地块
     */
    @PostMapping("/assign")
    @Transactional
    public Result<PlotAssignment> assignRecipe(
            @RequestParam Integer plotId,
            @Valid @RequestBody PlotAssignmentDTO dto) {
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) {
            throw new IllegalArgumentException("地块不存在: " + plotId);
        }
        Recipe recipe = recipeMapper.selectById(dto.getRecipeId());
        if (recipe == null) {
            throw new IllegalArgumentException("配方不存在: " + dto.getRecipeId());
        }
        
        // 取消之前的激活分配
        plotAssignmentMapper.deactivateByPlotId(plotId);
        
        // 创建新分配
        PlotAssignment assignment = new PlotAssignment();
        assignment.setPlotId(plotId);
        assignment.setRecipeId(dto.getRecipeId());
        Date now = new Date();
        assignment.setAssignedAt(now);
        assignment.setIsActive(true);
        assignment.setCreatedAt(now);
        assignment.setUpdatedAt(now);
        plotAssignmentMapper.insert(assignment);
        
        // 记录执行日志
        ExecutionLog log = new ExecutionLog();
        log.setPlotId(plotId);
        log.setRecipeId(dto.getRecipeId());
        log.setExecutions(dto.getExecutions() != null ? dto.getExecutions() : 1);
        log.setExecutionType("manual");
        Date now2 = new Date();
        log.setExecutedAt(now2);
        log.setCreatedAt(now2);
        executionLogMapper.insert(log);
        
        return Result.success(assignment);
    }
    
    /**
     * 获取地块的当前分配
     */
    @GetMapping("/assignment")
    public Result<PlotAssignment> getAssignment(@RequestParam Integer plotId) {
        PlotAssignment assignment = plotAssignmentMapper.findByPlotIdAndIsActiveTrue(plotId);
        return Result.success(assignment);
    }
    
    /**
     * 添加定时执行计划
     */
    @PostMapping("/schedules")
    @Transactional
    public Result<PlotSchedule> addSchedule(
            @RequestParam Integer plotId,
            @Valid @RequestBody PlotScheduleDTO dto) {
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) {
            throw new IllegalArgumentException("地块不存在: " + plotId);
        }
        Recipe recipe = recipeMapper.selectById(dto.getRecipeId());
        if (recipe == null) {
            throw new IllegalArgumentException("配方不存在: " + dto.getRecipeId());
        }
        
        PlotSchedule schedule = new PlotSchedule();
        schedule.setPlotId(plotId);
        schedule.setRecipeId(dto.getRecipeId());
        
        // 设置执行时间
        if (dto.getTimeHHmm() != null && !dto.getTimeHHmm().isEmpty()) {
            schedule.setScheduleTime(LocalTime.parse(dto.getTimeHHmm()));
        }
        
        schedule.setExecutions(dto.getExecutions() != null ? dto.getExecutions() : 1);
        schedule.setIsEnabled(true);
        Date now3 = new Date();
        schedule.setCreatedAt(now3);
        schedule.setUpdatedAt(now3);
        
        // 先插入基本字段
        plotScheduleMapper.insert(schedule);
        
        // 然后使用自定义SQL更新新字段（如果数据库支持）
        try {
            String scheduleType = dto.getScheduleType() != null ? dto.getScheduleType() : "daily";
            Integer dayOfWeek = dto.getDayOfWeek();
            Date scheduleDatetime = null;
            
            if (dto.getScheduleDatetime() != null && !dto.getScheduleDatetime().isEmpty()) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    scheduleDatetime = sdf.parse(dto.getScheduleDatetime());
                } catch (Exception e) {
                    throw new IllegalArgumentException("精确时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
                }
            }
            
            // 使用自定义SQL更新新字段
            plotScheduleMapper.updateScheduleFields(schedule.getId(), scheduleType, dayOfWeek, scheduleDatetime);
        } catch (Exception e) {
            // 如果更新失败（字段不存在），记录日志但不影响基本功能
            log.warn("更新定时任务扩展字段失败，可能数据库表未包含新字段: {}", e.getMessage());
        }
        
        return Result.success(schedule);
    }
    
    /**
     * 获取地块的定时计划列表
     */
    @GetMapping("/schedules")
    public Result<List<PlotSchedule>> getSchedules(@RequestParam Integer plotId) {
        List<PlotSchedule> schedules = plotScheduleMapper.findByPlotIdOrderByScheduleTimeAsc(plotId);
        return Result.success(schedules);
    }
    
    /**
     * 删除定时计划
     */
    @DeleteMapping("/schedules")
    @Transactional
    public Result<Void> deleteSchedule(@RequestParam Long scheduleId) {
        plotScheduleMapper.deleteById(scheduleId);
        return Result.success();
    }
    
    /**
     * 立即执行定时计划（发送MQTT消息）
     */
    @PostMapping("/schedules/execute")
    @Transactional
    public Result<String> executeSchedule(@RequestParam Long scheduleId) {
        PlotSchedule schedule = plotScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("定时计划不存在: " + scheduleId);
        }
        
        Recipe recipe = recipeMapper.selectById(schedule.getRecipeId());
        if (recipe == null) {
            throw new IllegalArgumentException("配方不存在: " + schedule.getRecipeId());
        }
        
        Plot plot = plotMapper.selectById(schedule.getPlotId());
        if (plot == null) {
            throw new IllegalArgumentException("地块不存在: " + schedule.getPlotId());
        }
        
        // 调用ScheduleTaskService执行
        scheduleTaskService.executeScheduleImmediately(schedule);
        
        // 记录执行日志
        ExecutionLog log = new ExecutionLog();
        log.setPlotId(schedule.getPlotId());
        log.setRecipeId(schedule.getRecipeId());
        log.setExecutions(schedule.getExecutions() != null ? schedule.getExecutions() : 1);
        log.setExecutionType("manual");
        log.setScheduleId(scheduleId);
        Date now = new Date();
        log.setExecutedAt(now);
        log.setCreatedAt(now);
        executionLogMapper.insert(log);
        
        return Result.success("执行成功，已发送MQTT消息到'time'主题");
    }
    
    /**
     * 立即执行配方分配（发送MQTT消息）
     */
    @PostMapping("/assign/execute")
    @Transactional
    public Result<String> executeAssignment(
            @RequestParam Integer plotId,
            @RequestParam(required = false) Integer executions) {
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) {
            throw new IllegalArgumentException("地块不存在: " + plotId);
        }
        
        PlotAssignment assignment = plotAssignmentMapper.findByPlotIdAndIsActiveTrue(plotId);
        if (assignment == null) {
            throw new IllegalArgumentException("地块未分配配方: " + plotId);
        }
        
        Recipe recipe = recipeMapper.selectById(assignment.getRecipeId());
        if (recipe == null) {
            throw new IllegalArgumentException("配方不存在: " + assignment.getRecipeId());
        }
        
        // 调用ScheduleTaskService执行
        scheduleTaskService.executeRecipeImmediately(plot, recipe, executions != null ? executions : 1);
        
        // 记录执行日志
        ExecutionLog log = new ExecutionLog();
        log.setPlotId(plotId);
        log.setRecipeId(assignment.getRecipeId());
        log.setExecutions(executions != null ? executions : 1);
        log.setExecutionType("manual");
        Date now = new Date();
        log.setExecutedAt(now);
        log.setCreatedAt(now);
        executionLogMapper.insert(log);
        
        return Result.success("执行成功，已发送MQTT消息到'time'主题");
    }
    
    /**
     * 获取所有地块的分配情况
     */
    @GetMapping("/assignments")
    public Result<Map<Integer, PlotAssignment>> getAllAssignments() {
        List<PlotAssignment> assignments = plotAssignmentMapper.selectList(null)
                .stream()
                .filter(PlotAssignment::getIsActive)
                .collect(Collectors.toList());
        // 需要查询地块信息来获取 plotNumber
        Map<Integer, PlotAssignment> map = assignments.stream()
                .collect(Collectors.toMap(
                        a -> {
                            Plot plot = plotMapper.selectById(a.getPlotId());
                            return plot != null ? plot.getPlotNumber() : a.getPlotId();
                        },
                        a -> a
                ));
        return Result.success(map);
    }
}
