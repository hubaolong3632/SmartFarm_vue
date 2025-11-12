package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 地块实体类
 * 对应表：plots
 */
@Data
@TableName("plots")
public class Plot {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 地块编号（1-4）
     */
    private Integer plotNumber;

    /**
     * 地块名称（可选）
     */
    private String name;

    /**
     * 状态（0禁用/1启用）
     */
    private Boolean status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

