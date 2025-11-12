package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 执行日志实体类
 * 对应表：execution_logs
 */
@Entity
@Table(name = "execution_logs", indexes = {
    @Index(name = "idx_plot_id", columnList = "plot_id"),
    @Index(name = "idx_recipe_id", columnList = "recipe_id"),
    @Index(name = "idx_executed_at", columnList = "executed_at"),
    @Index(name = "idx_execution_type", columnList = "execution_type")
})
@Data
@EntityListeners(AuditingEntityListener.class)
public class ExecutionLog {
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
     * 执行次数
     */
    @Column(nullable = false)
    private Integer executions;

    /**
     * 执行时间
     */
    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt;

    /**
     * 执行类型（manual手动/scheduled定时）
     */
    @Column(name = "execution_type", nullable = false, length = 20)
    private String executionType;

    /**
     * 关联的定时计划ID（如果是定时执行）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private PlotSchedule schedule;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
    }
}

