package com.greenhouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * AI报告DTO
 */
@Data
public class AiReportDTO {
    /**
     * 报告类型
     */
    @NotBlank(message = "报告类型不能为空")
    private String reportType;

    /**
     * 报告标题
     */
    private String reportTitle;

    /**
     * 报告内容
     */
    @NotBlank(message = "报告内容不能为空")
    private String reportContent;

    /**
     * 数据开始日期
     */
    private String startDate;

    /**
     * 数据结束日期
     */
    private String endDate;

    /**
     * 分析的数据条数
     */
    private Integer dataCount;
}

