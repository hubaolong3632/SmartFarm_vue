package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户实体类
 * 对应表：users
 */
@Data
@TableName("users")
public class User {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密后）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}

