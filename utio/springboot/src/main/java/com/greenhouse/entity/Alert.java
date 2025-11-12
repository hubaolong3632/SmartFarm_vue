package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 报警记录实体类
 * 对应表：alerts
 */
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_alert_type", columnList = "alert_type"),
    @Index(name = "idx_is_read", columnList = "is_read"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@EntityListeners(AuditingEntityListener.class)
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 报警级别（info/warning/error）
     */
    @Column(nullable = false, length = 20)
    private String level;

    /**
     * 报警消息
     */
    @Column(nullable = false, length = 500)
    private String message;

    /**
     * 报警类型（temperature/soil_moisture/light/automation等）
     */
    @Column(name = "alert_type", length = 50)
    private String alertType;

    /**
     * 关联数据（JSON格式）
     */
    @Column(name = "related_data", columnDefinition = "JSON")
    private String relatedData;

    /**
     * 是否已读（0未读/1已读）
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

