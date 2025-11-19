package com.greenhouse.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:greenhouse-secret-key-2024-very-long-secret-key-for-security}")
    private String secret;
    
    @Value("${jwt.expiration:86400000}") // 默认24小时
    private Long expiration;
    
    /**
     * 生成JWT token
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
    
    /**
     * 从token中获取Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("解析JWT token失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }
    
    /**
     * 从token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            } else if (userId instanceof Long) {
                return (Long) userId;
            }
        }
        return null;
    }
    
    /**
     * 验证token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return false;
            }
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("验证JWT token失败: {}", e.getMessage());
            return false;
        }
    }
}

