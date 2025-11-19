package com.greenhouse.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web配置类
 * 配置静态资源访问和拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final JwtInterceptor jwtInterceptor;
    
    /**
     * 配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(JwtConfig.getExcludePathPatterns());
    }
    
    /**
     * 配置静态资源处理器
     * 将 /file/** 映射到项目根目录下的 file 文件夹
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录下的 file 文件夹路径
        String currentPath = System.getProperty("user.dir") + File.separator + "file";
        
        // 转换为绝对路径并确保路径格式正确
        File fileDir = new File(currentPath);
        String absolutePath = fileDir.getAbsolutePath();
        
        // Windows路径需要转换为URL格式（将 \ 替换为 /），并确保末尾有斜杠
        String resourceLocation = "file:" + absolutePath.replace("\\", "/") + "/";
        
        // 注册资源处理器：/file/** 映射到 file: 协议路径
        registry.addResourceHandler("/file/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(0); // 禁用缓存，方便开发调试
        
        System.out.println("静态资源映射配置: /file/** -> " + resourceLocation);
    }
}

