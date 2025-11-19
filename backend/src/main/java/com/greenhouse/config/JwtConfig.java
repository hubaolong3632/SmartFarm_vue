package com.greenhouse.config;

import java.util.Arrays;
import java.util.List;

/**
 * JWT配置类
 * 统一管理JWT拦截器的排除路径
 */
public class JwtConfig {
    
    /**
     * 不需要JWT验证的路径列表
     * 注意：这里的路径是去掉 /api 前缀后的路径
     */
    public static final List<String> EXCLUDE_PATHS = Arrays.asList(
        "/auth/login",
        "/auth/verify",
        "/file/",
        "/images/file",
        "/automation/time",
        "/error"
    );
    
    /**
     * 获取排除路径数组（用于Spring拦截器配置）
     */
    public static String[] getExcludePathPatterns() {
        return EXCLUDE_PATHS.toArray(new String[0]);
    }
}

