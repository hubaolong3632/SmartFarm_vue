package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.PlotAssignmentDTO;
import com.greenhouse.dto.PlotScheduleDTO;
import com.greenhouse.entity.*;
import com.greenhouse.mapper.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        schedule.setScheduleTime(LocalTime.parse(dto.getTimeHHmm()));
        schedule.setExecutions(dto.getExecutions() != null ? dto.getExecutions() : 1);
        schedule.setIsEnabled(true);
        Date now3 = new Date();
        schedule.setCreatedAt(now3);
        schedule.setUpdatedAt(now3);
        plotScheduleMapper.insert(schedule);
        
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
