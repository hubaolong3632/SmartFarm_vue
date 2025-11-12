package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.PlotAssignmentDTO;
import com.greenhouse.dto.PlotScheduleDTO;
import com.greenhouse.entity.*;
import com.greenhouse.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地块管理控制器
 */
@RestController
@RequestMapping("/plots")
@RequiredArgsConstructor
public class PlotController {
    
    private final PlotRepository plotRepository;
    private final PlotAssignmentRepository plotAssignmentRepository;
    private final PlotScheduleRepository plotScheduleRepository;
    private final RecipeRepository recipeRepository;
    private final ExecutionLogRepository executionLogRepository;
    
    /**
     * 获取所有地块
     */
    @GetMapping
    public Result<List<Plot>> getAll() {
        return Result.success(plotRepository.findAll());
    }
    
    /**
     * 分配配方到地块
     */
    @PostMapping("/{plotId}/assign")
    @Transactional
    public Result<PlotAssignment> assignRecipe(
            @PathVariable Integer plotId,
            @Valid @RequestBody PlotAssignmentDTO dto) {
        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new IllegalArgumentException("地块不存在: " + plotId));
        Recipe recipe = recipeRepository.findById(dto.getRecipeId())
                .orElseThrow(() -> new IllegalArgumentException("配方不存在: " + dto.getRecipeId()));
        
        // 取消之前的激活分配
        plotAssignmentRepository.deactivateByPlotId(plotId);
        
        // 创建新分配
        PlotAssignment assignment = new PlotAssignment();
        assignment.setPlot(plot);
        assignment.setRecipe(recipe);
        assignment.setIsActive(true);
        PlotAssignment saved = plotAssignmentRepository.save(assignment);
        
        // 记录执行日志
        ExecutionLog log = new ExecutionLog();
        log.setPlot(plot);
        log.setRecipe(recipe);
        log.setExecutions(dto.getExecutions() != null ? dto.getExecutions() : 1);
        log.setExecutionType("manual");
        executionLogRepository.save(log);
        
        return Result.success(saved);
    }
    
    /**
     * 获取地块的当前分配
     */
    @GetMapping("/{plotId}/assignment")
    public Result<PlotAssignment> getAssignment(@PathVariable Integer plotId) {
        PlotAssignment assignment = plotAssignmentRepository.findByPlotIdAndIsActiveTrue(plotId)
                .orElse(null);
        return Result.success(assignment);
    }
    
    /**
     * 添加定时执行计划
     */
    @PostMapping("/{plotId}/schedules")
    @Transactional
    public Result<PlotSchedule> addSchedule(
            @PathVariable Integer plotId,
            @Valid @RequestBody PlotScheduleDTO dto) {
        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new IllegalArgumentException("地块不存在: " + plotId));
        Recipe recipe = recipeRepository.findById(dto.getRecipeId())
                .orElseThrow(() -> new IllegalArgumentException("配方不存在: " + dto.getRecipeId()));
        
        PlotSchedule schedule = new PlotSchedule();
        schedule.setPlot(plot);
        schedule.setRecipe(recipe);
        schedule.setScheduleTime(LocalTime.parse(dto.getTimeHHmm()));
        schedule.setExecutions(dto.getExecutions() != null ? dto.getExecutions() : 1);
        schedule.setIsEnabled(true);
        
        return Result.success(plotScheduleRepository.save(schedule));
    }
    
    /**
     * 获取地块的定时计划列表
     */
    @GetMapping("/{plotId}/schedules")
    public Result<List<PlotSchedule>> getSchedules(@PathVariable Integer plotId) {
        List<PlotSchedule> schedules = plotScheduleRepository.findByPlotIdOrderByScheduleTimeAsc(plotId);
        return Result.success(schedules);
    }
    
    /**
     * 删除定时计划
     */
    @DeleteMapping("/schedules/{scheduleId}")
    @Transactional
    public Result<Void> deleteSchedule(@PathVariable Long scheduleId) {
        plotScheduleRepository.deleteById(scheduleId);
        return Result.success();
    }
    
    /**
     * 获取所有地块的分配情况
     */
    @GetMapping("/assignments")
    public Result<Map<Integer, PlotAssignment>> getAllAssignments() {
        List<PlotAssignment> assignments = plotAssignmentRepository.findAll()
                .stream()
                .filter(PlotAssignment::getIsActive)
                .collect(Collectors.toList());
        Map<Integer, PlotAssignment> map = assignments.stream()
                .collect(Collectors.toMap(
                        a -> a.getPlot().getPlotNumber(),
                        a -> a
                ));
        return Result.success(map);
    }
}

