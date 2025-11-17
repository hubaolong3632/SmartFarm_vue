package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * AI分析报告实体类
 * 对应表：ai_reports
 */
@Data
@TableName("ai_reports")
public class AiReport {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 报告类型
     * image_analysis - 图片分析
     * sensor_analysis - 传感器数据分析
     * automation_advice - 自动化建议
     * comprehensive_report - 综合报告
     * auto_execution - 自动执行建议
     */
    private String reportType;

    /**
     * 报告标题
     */
    private String reportTitle;

    /**
     * 报告内容（Markdown格式）
     */
    private String reportContent;

    /**
     * 数据开始日期
     */
    private Date startDate;

    /**
     * 数据结束日期
     */
    private Date endDate;

    /**
     * 分析的数据条数
     */
    private Integer dataCount;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}

