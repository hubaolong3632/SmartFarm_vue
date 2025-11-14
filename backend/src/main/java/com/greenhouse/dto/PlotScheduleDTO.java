package com.greenhouse.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Date;

/**
 * 地块定时执行计划数据传输对象
 */
@Data
public class PlotScheduleDTO {
    @NotBlank(message = "配方ID不能为空")
    private String recipeId;
    
    // 兼容旧格式：HH:mm
    @Pattern(regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$", message = "时间格式错误，应为 HH:mm")
    private String timeHHmm;
    
    // 执行周期类型（daily每天/weekly每周/monthly每月）
    private String scheduleType;
    
    // 周几（0-6，0=周日，1=周一，...，6=周六）
    private Integer dayOfWeek;
    
    // 精确执行时间（年月日时分秒，格式：yyyy-MM-dd HH:mm:ss）
    private String scheduleDatetime;
    
    @NotNull(message = "执行次数不能为空")
    @Min(value = 1, message = "执行次数必须大于0")
    private Integer executions;
}

