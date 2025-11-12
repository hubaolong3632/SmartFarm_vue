package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 地块实体类
 * 对应表：plots
 */
@Entity
@Table(name = "plots")
@Data
@EntityListeners(AuditingEntityListener.class)
public class Plot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 地块编号（1-4）
     */
    @Column(name = "plot_number", nullable = false, unique = true)
    private Integer plotNumber;

    /**
     * 地块名称（可选）
     */
    @Column(length = 50)
    private String name;

    /**
     * 状态（0禁用/1启用）
     */
    @Column(nullable = false)
    private Boolean status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

