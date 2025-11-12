package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 自动化设置实体类
 * 对应表：automation_settings
 */
@Entity
@Table(name = "automation_settings")
@Data
@EntityListeners(AuditingEntityListener.class)
public class AutomationSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 设置键名
     */
    @Column(name = "setting_key", nullable = false, unique = true, length = 50)
    private String settingKey;

    /**
     * 设置值（JSON格式）
     */
    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    /**
     * 设置描述
     */
    @Column(length = 200)
    private String description;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

