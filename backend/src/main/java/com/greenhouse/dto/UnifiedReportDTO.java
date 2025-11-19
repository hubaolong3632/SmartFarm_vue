package com.greenhouse.dto;

import lombok.Data;

import java.util.Date;

/**
 * 统一的报告DTO
 * 用于合并AI报告和AI托管日志
 */
@Data
public class UnifiedReportDTO {
    /**
     * 报告类型：ai_report 或 ai_hosting_log
     */
    private String sourceType;
    
    /**
     * 报告ID
     */
    private Long id;
    
    /**
     * 报告类型（对于AI报告）或状态（对于托管日志）
     */
    private String reportType;
    
    /**
     * 报告标题
     */
    private String reportTitle;
    
    /**
     * 报告内容
     */
    private String reportContent;
    
    /**
     * 执行时间/创建时间
     */
    private Date executionTime;
    
    /**
     * 状态（仅用于托管日志）
     */
    private String status;
    
    /**
     * 执行的操作（JSON格式，仅用于托管日志）
     */
    private String actionsTaken;
    
    /**
     * 检测到的问题（JSON格式，仅用于托管日志）
     */
    private String issuesDetected;
    
    /**
     * 是否发送邮件（仅用于托管日志）
     */
    private Boolean emailSent;
    
    /**
     * 邮件内容（仅用于托管日志）
     */
    private String emailContent;
    
    /**
     * 执行耗时（毫秒，仅用于托管日志）
     */
    private Integer executionDurationMs;
    
    /**
     * 错误信息（仅用于托管日志）
     */
    private String errorMessage;
    
    /**
     * 数据开始日期（仅用于AI报告）
     */
    private Date startDate;
    
    /**
     * 数据结束日期（仅用于AI报告）
     */
    private Date endDate;
    
    /**
     * 数据条数（仅用于AI报告）
     */
    private Integer dataCount;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 更新时间（仅用于AI报告）
     */
    private Date updatedAt;
}

