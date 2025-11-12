package com.greenhouse.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 地块定时执行计划数据传输对象
 */
@Data
public class PlotScheduleDTO {
    @NotBlank(message = "配方ID不能为空")
    private String recipeId;
    
    @NotBlank(message = "执行时间不能为空")
    @Pattern(regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$", message = "时间格式错误，应为 HH:mm")
    private String timeHHmm;
    
    @NotNull(message = "执行次数不能为空")
    @Min(value = 1, message = "执行次数必须大于0")
    private Integer executions;
}

