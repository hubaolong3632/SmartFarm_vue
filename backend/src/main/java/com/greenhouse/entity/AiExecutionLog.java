package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * AI执行操作日志实体
 */
@Data
@TableName("ai_execution_logs")
public class AiExecutionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作类型（light/pump/recipe）
     */
    private String operationType;

    /**
     * 动作（on/off）
     */
    private String action;

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
     * AI建议原因
     */
    private String reason;

    /**
     * 原始Payload
     */
    private String payload;

    /**
     * 实际执行时间
     */
    private Date executeTime;

    /**
     * 创建时间
     */
    private Date createdAt;
}

