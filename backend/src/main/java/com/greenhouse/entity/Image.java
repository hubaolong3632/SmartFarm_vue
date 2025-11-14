package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 图片实体类
 * 对应表：images
 */
@Data
@TableName("images")
public class Image {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 记录时间（对应传感器数据时间）
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
     * 关联地块ID（可选）
     */
    private Integer plotId;

    /**
     * 是否异常（0否/1是）
     */
    private Boolean isAbnormal;

    /**
     * 异常原因
     */
    private String abnormalReason;

    /**
     * 创建时间
     */
    private Date createdAt;
}
