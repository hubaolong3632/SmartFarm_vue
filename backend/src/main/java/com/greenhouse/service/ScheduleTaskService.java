package com.greenhouse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.entity.Plot;
import com.greenhouse.entity.PlotSchedule;
import com.greenhouse.entity.Recipe;
import com.greenhouse.mapper.PlotMapper;
import com.greenhouse.mapper.PlotScheduleMapper;
import com.greenhouse.mapper.RecipeMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务调度服务
 * 负责检查定时任务并执行，通过MQTT发送配方数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleTaskService {

    private final PlotScheduleMapper plotScheduleMapper;
    private final RecipeMapper recipeMapper;
    private final PlotMapper plotMapper;
    private final MqttService mqttService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化服务
     */
    @PostConstruct
    public void init() {
        log.info("定时任务调度服务已启动");
    }

    /**
     * 每10秒检查一次定时任务
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkAndExecuteSchedules() {
        try {
            // 获取所有启用的定时任务
            List<PlotSchedule> schedules = plotScheduleMapper.findByIsEnabledTrueOrderByScheduleTimeAsc();
            
            LocalDateTime now = LocalDateTime.now();
            Date nowDate = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
            
            for (PlotSchedule schedule : schedules) {
                if (shouldExecute(schedule, now)) {
                    executeSchedule(schedule, nowDate);
                }
            }
        } catch (Exception e) {
            log.error("检查定时任务失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 判断是否应该执行定时任务
     */
    private boolean shouldExecute(PlotSchedule schedule, LocalDateTime now) {
        if (!schedule.getIsEnabled()) {
            return false;
        }

        String scheduleType = schedule.getScheduleType() != null ? schedule.getScheduleType() : "daily";
        Date scheduleDatetime = schedule.getScheduleDatetime();
        Integer dayOfWeek = schedule.getDayOfWeek();
        Date lastExecutedAt = schedule.getLastExecutedAt();

        // 提取当前时间的小时和分钟（在整个方法中使用）
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();

        // 如果设置了精确时间，使用精确时间判断
        if (scheduleDatetime != null) {
            LocalDateTime scheduleTime = LocalDateTime.ofInstant(
                scheduleDatetime.toInstant(), 
                ZoneId.systemDefault()
            );
            
            // 检查是否到了执行时间（允许10秒误差）
            if (now.isAfter(scheduleTime.minusSeconds(5)) && now.isBefore(scheduleTime.plusSeconds(10))) {
                // 检查是否已经执行过（避免重复执行）
                if (lastExecutedAt != null) {
                    LocalDateTime lastExecuted = LocalDateTime.ofInstant(
                        lastExecutedAt.toInstant(),
                        ZoneId.systemDefault()
                    );
                    // 如果上次执行时间在本次计划时间之后，说明已经执行过了
                    if (lastExecuted.isAfter(scheduleTime.minusSeconds(5))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        // 如果没有精确时间，使用scheduleTime和scheduleType判断
        if (schedule.getScheduleTime() == null) {
            return false;
        }

        int scheduleHour = schedule.getScheduleTime().getHour();
        int scheduleMinute = schedule.getScheduleTime().getMinute();

        // 检查时间是否匹配（允许1分钟误差）
        if (Math.abs((currentHour * 60 + currentMinute) - (scheduleHour * 60 + scheduleMinute)) > 1) {
            return false;
        }

        // 检查是否已经执行过（今天）
        if (lastExecutedAt != null) {
            LocalDateTime lastExecuted = LocalDateTime.ofInstant(
                lastExecutedAt.toInstant(),
                ZoneId.systemDefault()
            );
            if (lastExecuted.toLocalDate().equals(now.toLocalDate())) {
                return false;
            }
        }

        // 根据执行周期类型判断
        switch (scheduleType.toLowerCase()) {
            case "daily":
                // 每天执行
                return true;
            case "weekly":
                // 每周执行，检查周几
                if (dayOfWeek != null) {
                    int currentDayOfWeek = now.getDayOfWeek().getValue(); // 1=周一，7=周日
                    int scheduleDayOfWeek = dayOfWeek == 0 ? 7 : dayOfWeek; // 转换为1-7
                    return currentDayOfWeek == scheduleDayOfWeek;
                }
                return false;
            case "monthly":
                // 每月执行，检查日期和时间
                if (scheduleDatetime != null) {
                    LocalDateTime scheduleTime = LocalDateTime.ofInstant(
                        scheduleDatetime.toInstant(),
                        ZoneId.systemDefault()
                    );
                    // 检查日期是否匹配，并且时间在允许范围内
                    if (now.getDayOfMonth() == scheduleTime.getDayOfMonth()) {
                        int scheduleTimeHour = scheduleTime.getHour();
                        int scheduleTimeMinute = scheduleTime.getMinute();
                        // 允许1分钟误差
                        return Math.abs((currentHour * 60 + currentMinute) - (scheduleTimeHour * 60 + scheduleTimeMinute)) <= 1;
                    }
                } else if (schedule.getScheduleTime() != null) {
                    // 如果没有精确时间，使用scheduleTime，每月同一天执行
                    // 这里需要记录上次执行的日期，避免同一天重复执行
                    if (lastExecutedAt != null) {
                        LocalDateTime lastExecuted = LocalDateTime.ofInstant(
                            lastExecutedAt.toInstant(),
                            ZoneId.systemDefault()
                        );
                        // 如果上次执行是在本月，则不执行
                        if (lastExecuted.getYear() == now.getYear() && 
                            lastExecuted.getMonth() == now.getMonth()) {
                            return false;
                        }
                    }
                    // 检查时间是否匹配（使用已定义的变量）
                    return Math.abs((currentHour * 60 + currentMinute) - (scheduleHour * 60 + scheduleMinute)) <= 1;
                }
                return false;
            default:
                return false;
        }
    }

    /**
     * 立即执行定时任务（公开方法，供Controller调用）
     */
    public void executeScheduleImmediately(PlotSchedule schedule) {
        executeSchedule(schedule, new Date());
    }
    
    /**
     * 立即执行配方（公开方法，供Controller调用）
     */
    public void executeRecipeImmediately(Plot plot, Recipe recipe, Integer executions) {
        try {
            log.info("立即执行配方: plotId={}, recipeId={}, executions={}", 
                plot.getId(), recipe.getId(), executions);

            // 构建MQTT消息JSON
            Map<String, Object> message = new HashMap<>();
            message.put("plotId", plot.getId());
            message.put("plotName", plot.getName() != null ? plot.getName() : "地块" + plot.getPlotNumber());
            message.put("recipeId", recipe.getId());
            message.put("recipeName", recipe.getName());
            message.put("waterMl", recipe.getWaterMl() != null ? recipe.getWaterMl() : 0);
            message.put("nutrientMl", recipe.getNutrientMl() != null ? recipe.getNutrientMl() : 0);
            message.put("rootingPowderMl", recipe.getRootingPowderMl() != null ? recipe.getRootingPowderMl() : 0);
            message.put("specialMl", recipe.getSpecialMl() != null ? recipe.getSpecialMl() : 0);
            message.put("executions", executions != null ? executions : 1);
            message.put("executeTime", System.currentTimeMillis());

            // 转换为JSON字符串
            String jsonMessage = objectMapper.writeValueAsString(message);

            // 发送到MQTT主题 "time"
            mqttService.publish("time", jsonMessage, 1);
            log.info("已发送配方数据到MQTT主题 'time': {}", jsonMessage);

        } catch (Exception e) {
            log.error("立即执行配方失败: plotId={}, recipeId={}, error={}", 
                plot.getId(), recipe.getId(), e.getMessage(), e);
            throw new RuntimeException("执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行定时任务
     */
    private void executeSchedule(PlotSchedule schedule, Date executeTime) {
        try {
            log.info("执行定时任务: scheduleId={}, plotId={}, recipeId={}", 
                schedule.getId(), schedule.getPlotId(), schedule.getRecipeId());

            // 获取配方信息
            Recipe recipe = recipeMapper.selectById(schedule.getRecipeId());
            if (recipe == null) {
                log.error("配方不存在: recipeId={}", schedule.getRecipeId());
                return;
            }

            // 获取地块信息
            Plot plot = plotMapper.selectById(schedule.getPlotId());
            if (plot == null) {
                log.error("地块不存在: plotId={}", schedule.getPlotId());
                return;
            }

            // 构建MQTT消息JSON
            Map<String, Object> message = new HashMap<>();
            message.put("plotId", plot.getId());
            message.put("plotName", plot.getName() != null ? plot.getName() : "地块" + plot.getPlotNumber());
            message.put("recipeId", recipe.getId());
            message.put("recipeName", recipe.getName());
            message.put("waterMl", recipe.getWaterMl() != null ? recipe.getWaterMl() : 0);
            message.put("nutrientMl", recipe.getNutrientMl() != null ? recipe.getNutrientMl() : 0);
            message.put("rootingPowderMl", recipe.getRootingPowderMl() != null ? recipe.getRootingPowderMl() : 0);
            message.put("specialMl", recipe.getSpecialMl() != null ? recipe.getSpecialMl() : 0);
            message.put("executions", schedule.getExecutions() != null ? schedule.getExecutions() : 1);
            message.put("executeTime", executeTime.getTime());

            // 转换为JSON字符串
            String jsonMessage = objectMapper.writeValueAsString(message);

            // 发送到MQTT主题 "time"
            mqttService.publish("time", jsonMessage, 1);
            log.info("已发送配方数据到MQTT主题 'time': {}", jsonMessage);

            // 更新上次执行时间
            schedule.setLastExecutedAt(executeTime);
            plotScheduleMapper.updateById(schedule);

            // 记录执行日志（可选，如果需要的话）
            // executionLogService.create(...)

        } catch (Exception e) {
            log.error("执行定时任务失败: scheduleId={}, error={}", schedule.getId(), e.getMessage(), e);
        }
    }
}

