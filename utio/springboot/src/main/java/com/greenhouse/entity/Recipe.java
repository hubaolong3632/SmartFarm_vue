package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 配方实体类
 * 对应表：recipes
 */
@Entity
@Table(name = "recipes", indexes = {
    @Index(name = "idx_name", columnList = "name")
})
@Data
@EntityListeners(AuditingEntityListener.class)
public class Recipe {
    @Id
    @Column(length = 50)
    private String id;

    /**
     * 配方名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 水（毫升）
     */
    @Column(name = "water_ml", nullable = false)
    private Integer waterMl;

    /**
     * 营养液（毫升）
     */
    @Column(name = "nutrient_ml", nullable = false)
    private Integer nutrientMl;

    /**
     * 生根粉（毫升）
     */
    @Column(name = "rooting_powder_ml", nullable = false)
    private Integer rootingPowderMl;

    /**
     * 特殊营养（毫升）
     */
    @Column(name = "special_ml", nullable = false)
    private Integer specialMl;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

