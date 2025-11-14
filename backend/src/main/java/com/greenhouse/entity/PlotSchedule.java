package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
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
     * 执行周期类型（daily每天/weekly每周/monthly每月）
     * exist = false 表示如果字段不存在则忽略
     */
    @TableField(exist = false)
    private String scheduleType;

    /**
     * 周几（0-6，0=周日，1=周一，...，6=周六）
     * exist = false 表示如果字段不存在则忽略
     */
    @TableField(exist = false)
    private Integer dayOfWeek;

    /**
     * 精确执行时间（年月日时分秒）
     * exist = false 表示如果字段不存在则忽略
     */
    @TableField(exist = false)
    private Date scheduleDatetime;

    /**
     * 上次执行时间
     * exist = false 表示如果字段不存在则忽略
     */
    @TableField(exist = false)
    private Date lastExecutedAt;

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
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
