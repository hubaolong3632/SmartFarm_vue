package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 地块定时执行计划实体类
 * 对应表：plot_schedules
 */
@Data
@TableName("plot_schedules")
public class PlotSchedule {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 地块ID
     */
    private Integer plotId;

    /**
     * 配方ID
     */
    private String recipeId;

    /**
     * 执行时间（HH:mm格式）
     */
    private LocalTime scheduleTime;

    /**
     * 执行次数
     */
    private Integer executions;

    /**
     * 是否启用（0否/1是）
     */
    private Boolean isEnabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
