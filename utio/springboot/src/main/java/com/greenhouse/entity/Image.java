package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图片实体类
 * 对应表：images
 */
@Entity
@Table(name = "images", indexes = {
    @Index(name = "idx_record_time", columnList = "record_time"),
    @Index(name = "idx_plot_id", columnList = "plot_id"),
    @Index(name = "idx_is_abnormal", columnList = "is_abnormal"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@EntityListeners(AuditingEntityListener.class)
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 图片URL
     */
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /**
     * 记录时间（对应传感器数据时间）
     */
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    /**
     * 温度（摄氏度）
     */
    @Column(name = "temperature_c", precision = 5, scale = 2)
    private BigDecimal temperatureC;

    /**
     * 土壤湿度（百分比）
     */
    @Column(name = "soil_moisture_pct", precision = 5, scale = 2)
    private BigDecimal soilMoisturePct;

    /**
     * 光照强度（lux）
     */
    @Column(name = "light_lux")
    private Integer lightLux;

    /**
     * 关联地块ID（可选）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id")
    private Plot plot;

    /**
     * 是否异常（0否/1是）
     */
    @Column(name = "is_abnormal", nullable = false)
    private Boolean isAbnormal;

    /**
     * 异常原因
     */
    @Column(name = "abnormal_reason", length = 200)
    private String abnormalReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

