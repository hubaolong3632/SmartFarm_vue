package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 地块定时执行计划实体类
 * 对应表：plot_schedules
 */
@Entity
@Table(name = "plot_schedules", indexes = {
    @Index(name = "idx_plot_id", columnList = "plot_id"),
    @Index(name = "idx_recipe_id", columnList = "recipe_id"),
    @Index(name = "idx_schedule_time", columnList = "schedule_time"),
    @Index(name = "idx_is_enabled", columnList = "is_enabled")
})
@Data
@EntityListeners(AuditingEntityListener.class)
public class PlotSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 地块ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id", nullable = false)
    private Plot plot;

    /**
     * 配方ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    /**
     * 执行时间（HH:mm格式）
     */
    @Column(name = "schedule_time", nullable = false)
    private LocalTime scheduleTime;

    /**
     * 执行次数
     */
    @Column(nullable = false)
    private Integer executions;

    /**
     * 是否启用（0否/1是）
     */
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

