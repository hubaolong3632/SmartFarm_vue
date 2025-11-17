package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.util.ImageDataInserter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 图片数据管理控制器
 * 用于插入测试数据
 */
@RestController
@RequestMapping("/images/data")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ImageDataController {
    
    private final ImageDataInserter imageDataInserter;
    
    /**
     * 插入基本图片数据
     * 用于测试和初始化
     */
    @PostMapping("/insert-sample")
    public Result<String> insertSampleImages() {
        try {
            imageDataInserter.insertSampleImages();
            return Result.success("图片数据插入成功");
        } catch (Exception e) {
            return Result.error("图片数据插入失败: " + e.getMessage());
        }
    }
}

