package com.greenhouse.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 配方数据传输对象
 */
@Data
public class RecipeDTO {
    // ID 在创建时可以为空，由后端自动生成
    private String id;
    
    @NotBlank(message = "配方名称不能为空")
    private String name;
    
    @NotNull(message = "水量不能为空")
    @Min(value = 0, message = "水量不能为负数")
    private Integer waterMl;
    
    @NotNull(message = "营养液量不能为空")
    @Min(value = 0, message = "营养液量不能为负数")
    private Integer nutrientMl;
    
    @NotNull(message = "生根粉量不能为空")
    @Min(value = 0, message = "生根粉量不能为负数")
    private Integer rootingPowderMl;
    
    @NotNull(message = "特殊营养量不能为空")
    @Min(value = 0, message = "特殊营养量不能为负数")
    private Integer specialMl;
}

