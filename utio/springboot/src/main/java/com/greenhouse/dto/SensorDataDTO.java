package com.greenhouse.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 传感器数据传输对象
 */
@Data
public class SensorDataDTO {
    private Long id;
    
    @NotNull(message = "记录时间不能为空")
    private LocalDateTime recordTime;
    
    @NotNull(message = "温度不能为空")
    @DecimalMin(value = "-50", message = "温度不能低于-50°C")
    @DecimalMax(value = "100", message = "温度不能高于100°C")
    private BigDecimal temperatureC;
    
    @NotNull(message = "土壤湿度不能为空")
    @DecimalMin(value = "0", message = "土壤湿度不能小于0")
    @DecimalMax(value = "100", message = "土壤湿度不能大于100")
    private BigDecimal soilMoisturePct;
    
    @NotNull(message = "光照强度不能为空")
    private Integer lightLux;
    
    @NotNull(message = "是否下雨不能为空")
    private Boolean isRaining;
}

