package com.greenhouse.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 图片数据传输对象
 */
@Data
public class ImageDTO {
    @NotBlank(message = "图片URL不能为空")
    private String url;
    
    @NotNull(message = "记录时间不能为空")
    private Date recordTime;
    
    private BigDecimal temperatureC;
    
    private BigDecimal humidityPct;
    
    private BigDecimal soilMoisturePct;
    
    private Integer lightLux;
    
    private Boolean isRaining;
    
    private BigDecimal oxygenPct;
    
    private Integer co2Ppm;
    
    private Integer plotId;
}

