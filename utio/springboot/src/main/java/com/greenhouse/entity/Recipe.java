package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配方实体类
 * 对应表：recipes
 */
@Data
@TableName("recipes")
public class Recipe {
    /**
     * 配方ID
     */
    @TableId
    private String id;

    /**
     * 配方名称
     */
    private String name;

    /**
     * 水（毫升）
     */
    private Integer waterMl;

    /**
     * 营养液（毫升）
     */
    private Integer nutrientMl;

    /**
     * 生根粉（毫升）
     */
    private Integer rootingPowderMl;

    /**
     * 特殊营养（毫升）
     */
    private Integer specialMl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

