package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * AI托管执行日志实体类
 */
@Data
@TableName("ai_hosting_logs")
public class AiHostingLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Date executionTime;
    
    private String status;
    
    private String actionsTaken; // JSON格式
    
    private String issuesDetected; // JSON格式
    
    private Boolean emailSent;
    
    private String emailContent;
    
    private Integer executionDurationMs;
    
    private String errorMessage;
    
    private Date createdAt;
}

