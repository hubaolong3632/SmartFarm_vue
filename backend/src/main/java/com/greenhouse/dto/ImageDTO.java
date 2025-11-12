package com.greenhouse.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图片数据传输对象
 */
@Data
public class ImageDTO {
    @NotBlank(message = "图片URL不能为空")
    private String url;
    
    @NotNull(message = "记录时间不能为空")
    private LocalDateTime recordTime;
    
    private BigDecimal temperatureC;
    
    private BigDecimal soilMoisturePct;
    
    private Integer lightLux;
    
    private Integer plotId;
}

