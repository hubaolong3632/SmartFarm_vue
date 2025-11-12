package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 地块配方分配实体类
 * 对应表：plot_assignments
 */
@Data
@TableName("plot_assignments")
public class PlotAssignment {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 地块ID
     */
    private Integer plotId;

    /**
     * 配方ID
     */
    private String recipeId;

    /**
     * 分配时间
     */
    private LocalDateTime assignedAt;

    /**
     * 是否激活（0否/1是）
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

