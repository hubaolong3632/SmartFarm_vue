package com.greenhouse.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 地块配方分配数据传输对象
 */
@Data
public class PlotAssignmentDTO {
    @NotBlank(message = "配方ID不能为空")
    private String recipeId;
    
    @NotNull(message = "执行次数不能为空")
    @Min(value = 1, message = "执行次数必须大于0")
    private Integer executions;
}

