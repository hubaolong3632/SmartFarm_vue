package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 传感器数据实体类
 * 对应表：sensor_data
 */
@Data
@TableName("sensor_data")
public class SensorData {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 记录时间（精确到小时）
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
     * 是否下雨（0否/1是）
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
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}

