package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行日志实体类
 * 对应表：execution_logs
 */
@Data
@TableName("execution_logs")
public class ExecutionLog {
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
     * 执行次数
     */
    private Integer executions;

    /**
     * 执行时间
     */
    private LocalDateTime executedAt;

    /**
     * 执行类型（manual手动/scheduled定时）
     */
    private String executionType;

    /**
     * 关联的定时计划ID（如果是定时执行）
     */
    private Long scheduleId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
