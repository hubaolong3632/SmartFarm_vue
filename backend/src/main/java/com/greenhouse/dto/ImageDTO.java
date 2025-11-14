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
    /**
     * 图片URL（保存接口必填，上传接口不需要）
     */
    @NotBlank(message = "图片URL不能为空")
    private String url;
    
    /**
     * 记录时间（可选，不提供则使用当前时间）
     */
    private Date recordTime;
    
    /**
     * 温度（摄氏度）
     */
    private BigDecimal temperatureC;
    
    /**
     * 湿度（百分比）
     */
    private BigDecimal humidityPct;
    
    /**
     * 土壤湿度（百分比）
     */
    private BigDecimal soilMoisturePct;
    
    /**
     * 光照强度（lux）
     */
    private Integer lightLux;
    
    /**
     * 是否下雨
     */
    private Boolean isRaining;
    
    /**
     * 氧气含量（百分比）
     */
    private BigDecimal oxygenPct;
    
    /**
     * 二氧化碳含量（ppm）
     */
    private Integer co2Ppm;
    
    /**
     * 关联地块ID（可选）
     */
    private Integer plotId;
}

