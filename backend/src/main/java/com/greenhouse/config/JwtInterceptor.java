package com.greenhouse.config;

import com.greenhouse.common.Result;
import com.greenhouse.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT拦截器
 * 用于验证请求中的JWT token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 处理OPTIONS预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            response.setStatus(HttpServletResponse.SC_OK);
            return false;
        }
        
        String path = request.getRequestURI();
        
        // 记录请求路径（用于调试）
//        log.debug("拦截请求路径: {} {}", request.getMethod(), path);
        
        // 检查是否为排除路径
        if (isExcludePath(path)) {
//            log.debug("路径已排除，跳过验证: {}", path);
            return true;
        }
        
        // 获取token
        String token = getTokenFromRequest(request);
        
        if (token == null) {
            log.warn("请求缺少token: {} {}", request.getMethod(), path);
            return handleUnauthorized(response, "缺少认证token");
        }
        
        // 验证token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token无效或已过期: {} {}", request.getMethod(), path);
            return handleUnauthorized(response, "Token无效或已过期");
        }
        
        // 将用户信息存储到request中，方便后续使用
        String username = jwtUtil.getUsernameFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        
//        log.debug("Token验证通过，用户: {}", username);
        return true;
    }
    
    /**
     * 从请求中获取token
     * 支持从以下位置获取：
     * 1. Authorization header (Bearer token)
     * 2. URL参数 token (用于SSE等不支持自定义header的场景)
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 优先从 Authorization header 获取
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        
        // 如果header中没有，尝试从URL参数获取（用于SSE等场景）
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }
        
        return null;
    }
    
    /**
     * 检查是否为排除路径
     */
    private boolean isExcludePath(String path) {
        // 移除上下文路径（如果有）
        if (path.startsWith("/api")) {
            path = path.substring(4);
        }
        
        // 确保路径以 / 开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        for (String excludePath : JwtConfig.EXCLUDE_PATHS) {
            // 支持精确匹配和前缀匹配
            if (path.equals(excludePath) || path.startsWith(excludePath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 处理未授权请求
     */
    private boolean handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        // 设置CORS响应头
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        Result<?> result = Result.error(11111, message);
        String jsonResponse = objectMapper.writeValueAsString(result);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
        
        return false;
    }
}

