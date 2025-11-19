package com.greenhouse.service;

import com.greenhouse.entity.User;
import com.greenhouse.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    
    /**
     * 根据用户名查找用户
     */
    public User findByUsername(String username) {
        return userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        );
    }
    
    /**
     * 验证密码（明文比较）
     */
    public boolean verifyPassword(String rawPassword, String storedPassword) {
        return rawPassword != null && rawPassword.equals(storedPassword);
    }
    
    /**
     * 创建用户（用于初始化管理员账户）
     */
    @Transactional
    public User createUser(String username, String password, String nickname, String email) {
        User existingUser = findByUsername(username);
        if (existingUser != null) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // 直接存储明文密码
        user.setNickname(nickname);
        user.setEmail(email);
        Date now = new Date();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        
        userMapper.insert(user);
        log.info("创建用户成功: {}", username);
        return user;
    }
}

