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
    
    // 记录时间（MQTT 数据可能不包含，使用当前时间）
    private LocalDateTime recordTime;
    
    // 温度（可选，MQTT 数据可能不包含所有字段）
    @DecimalMin(value = "-50", message = "温度不能低于-50°C")
    @DecimalMax(value = "100", message = "温度不能高于100°C")
    private BigDecimal temperatureC;
    
    // 土壤湿度（可选）
    @DecimalMin(value = "0", message = "土壤湿度不能小于0")
    @DecimalMax(value = "100", message = "土壤湿度不能大于100")
    private BigDecimal soilMoisturePct;
    
    // 光照强度（可选）
    private Integer lightLux;
    
    // 是否下雨（可选）
    private Boolean isRaining;
}

