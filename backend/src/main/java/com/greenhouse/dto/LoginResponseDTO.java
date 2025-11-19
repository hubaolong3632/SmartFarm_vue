package com.greenhouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String username;
    private String nickname;
    private Long userId;
}

