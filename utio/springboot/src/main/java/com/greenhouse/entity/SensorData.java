package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 传感器数据实体类
 * 对应表：sensor_data
 */
@Entity
@Table(name = "sensor_data", indexes = {
    @Index(name = "idx_record_time", columnList = "record_time"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@EntityListeners(AuditingEntityListener.class)
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 记录时间（精确到小时）
     */
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    /**
     * 温度（摄氏度）
     */
    @Column(name = "temperature_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal temperatureC;

    /**
     * 土壤湿度（百分比）
     */
    @Column(name = "soil_moisture_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal soilMoisturePct;

    /**
     * 光照强度（lux）
     */
    @Column(name = "light_lux", nullable = false)
    private Integer lightLux;

    /**
     * 是否下雨（0否/1是）
     */
    @Column(name = "is_raining", nullable = false)
    private Boolean isRaining;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

