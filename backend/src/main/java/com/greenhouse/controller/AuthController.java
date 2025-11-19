package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.LoginDTO;
import com.greenhouse.dto.LoginResponseDTO;
import com.greenhouse.entity.User;
import com.greenhouse.service.UserService;
import com.greenhouse.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AuthController {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            // 查找用户
            User user = userService.findByUsername(loginDTO.getUsername());
            if (user == null) {
                return Result.error(401, "用户名或密码错误");
            }
            
            // 验证密码
            if (!userService.verifyPassword(loginDTO.getPassword(), user.getPassword())) {
                return Result.error(401, "用户名或密码错误");
            }
            
            // 生成JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            // 返回登录信息
            LoginResponseDTO response = new LoginResponseDTO(
                token,
                user.getUsername(),
                user.getNickname(),
                user.getId()
            );
            
            log.info("用户登录成功: {}", user.getUsername());
            return Result.success(response);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
            return Result.error(500, "登录失败，请稍后重试");
        }
    }
    
    /**
     * 验证token（用于前端检查token是否有效）
     */
    @GetMapping("/verify")
    public Result<Boolean> verify(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(11111, "Token无效");
        }
        
        String token = authorization.substring(7);
        if (jwtUtil.validateToken(token)) {
            return Result.success(true);
        } else {
            return Result.error(11111, "Token已过期");
        }
    }
}

