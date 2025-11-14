package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.ImageDTO;
import com.greenhouse.entity.Image;
import com.greenhouse.mapper.ImageMapper;
import com.greenhouse.mapper.PlotMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 图片管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ImageController {
    
    private final ImageMapper imageMapper;
    private final PlotMapper plotMapper;
    
    @Value("${file.upload.path:./uploads/images}")
    private String uploadPath;
    
    @Value("${file.upload.url-prefix:/api/images/files/}")
    private String urlPrefix;
    
    /**
     * 创建图片记录
     */
    @PostMapping
    @Transactional
    public Result<Image> create(@Valid @RequestBody ImageDTO dto) {
        Image image = new Image();
        image.setImageUrl(dto.getUrl());
        image.setRecordTime(dto.getRecordTime());
        image.setTemperatureC(dto.getTemperatureC());
        image.setHumidityPct(dto.getHumidityPct());
        image.setSoilMoisturePct(dto.getSoilMoisturePct());
        image.setLightLux(dto.getLightLux());
        image.setIsRaining(dto.getIsRaining());
        image.setOxygenPct(dto.getOxygenPct());
        image.setCo2Ppm(dto.getCo2Ppm());
        image.setPlotId(dto.getPlotId());
        
        // 判断是否异常：只有温度高于舒适温度（35°C）或土壤湿度低于10%才标记为异常
        boolean isAbnormal = false;
        StringBuilder abnormalReason = new StringBuilder();
        // 温度高于舒适温度（高阈值，默认35°C）
        if (dto.getTemperatureC() != null && 
            dto.getTemperatureC().compareTo(new BigDecimal("35")) > 0) {
            isAbnormal = true;
            abnormalReason.append("温度异常");
        }
        // 土壤湿度低于10%
        if (dto.getSoilMoisturePct() != null && 
            dto.getSoilMoisturePct().compareTo(new BigDecimal("10")) < 0) {
            isAbnormal = true;
            if (abnormalReason.length() > 0) abnormalReason.append(", ");
            abnormalReason.append("土壤湿度异常");
        }
        image.setIsAbnormal(isAbnormal);
        image.setAbnormalReason(abnormalReason.length() > 0 ? abnormalReason.toString() : null);
        image.setCreatedAt(new Date());
        
        imageMapper.insert(image);
        return Result.success(image);
    }
    
    /**
     * 批量创建图片记录
     */
    @PostMapping("/batch")
    @Transactional
    public Result<List<Image>> batchCreate(@Valid @RequestBody List<ImageDTO> dtos) {
        List<Image> images = dtos.stream().map(dto -> {
            Image image = new Image();
            image.setImageUrl(dto.getUrl());
            image.setRecordTime(dto.getRecordTime());
            image.setTemperatureC(dto.getTemperatureC());
            image.setHumidityPct(dto.getHumidityPct());
            image.setSoilMoisturePct(dto.getSoilMoisturePct());
            image.setLightLux(dto.getLightLux());
            image.setIsRaining(dto.getIsRaining());
            image.setOxygenPct(dto.getOxygenPct());
            image.setCo2Ppm(dto.getCo2Ppm());
            image.setPlotId(dto.getPlotId());
            
            // 判断是否异常：只有温度高于舒适温度（35°C）或土壤湿度低于10%才标记为异常
            boolean isAbnormal = false;
            StringBuilder abnormalReason = new StringBuilder();
            // 温度高于舒适温度（高阈值，默认35°C）
            if (dto.getTemperatureC() != null && 
                dto.getTemperatureC().compareTo(new BigDecimal("35")) > 0) {
                isAbnormal = true;
                abnormalReason.append("温度异常");
            }
            // 土壤湿度低于10%
            if (dto.getSoilMoisturePct() != null && 
                dto.getSoilMoisturePct().compareTo(new BigDecimal("10")) < 0) {
                isAbnormal = true;
                if (abnormalReason.length() > 0) abnormalReason.append(", ");
                abnormalReason.append("土壤湿度异常");
            }
            image.setIsAbnormal(isAbnormal);
            image.setAbnormalReason(abnormalReason.length() > 0 ? abnormalReason.toString() : null);
            image.setCreatedAt(new Date());
            imageMapper.insert(image);
            return image;
        }).toList();
        return Result.success(images);
    }
    
    /**
     * 根据日期查询图片
     */
    @GetMapping("/date")
    public Result<List<Image>> getByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        List<Image> images = imageMapper.findByDate(date);
        return Result.success(images);
    }
    
    /**
     * 获取异常图片
     */
    @GetMapping("/abnormal")
    public Result<List<Image>> getAbnormal() {
        return Result.success(imageMapper.findByIsAbnormalTrueOrderByRecordTimeDesc());
    }
    
    /**
     * 获取指定地块的图片
     */
    @GetMapping("/plot")
    public Result<List<Image>> getByPlotId(@RequestParam Integer plotId) {
        return Result.success(imageMapper.findByPlotIdOrderByRecordTimeDesc(plotId));
    }
    
    /**
     * 图片上传接口
     * 上传图片文件并返回图片URL
     */
    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传的文件为空");
        }
        
        try {
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;
            
            // 保存文件
            Path filePath = uploadDir.resolve(filename);
            Files.write(filePath, file.getBytes());
            
            // 生成访问URL
            String imageUrl = urlPrefix + filename;
            
            log.info("图片上传成功: {} -> {}", originalFilename, imageUrl);
            return Result.success(imageUrl);
            
        } catch (IOException e) {
            log.error("图片上传失败: {}", e.getMessage(), e);
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存图片记录（包含传感器数据）
     * 通过接口插入：温度、湿度、光照、是否下雨、氧气、二氧化碳和图片链接
     */
    @PostMapping("/save")
    @Transactional
    public Result<Image> saveImageWithData(@Valid @RequestBody ImageDTO dto) {
        try {
            Image image = new Image();
            image.setImageUrl(dto.getUrl());
            
            // 记录时间：如果未提供，使用当前时间
            if (dto.getRecordTime() != null) {
                image.setRecordTime(dto.getRecordTime());
            } else {
                image.setRecordTime(new Date());
            }
            
            // 传感器数据
            image.setTemperatureC(dto.getTemperatureC());
            image.setHumidityPct(dto.getHumidityPct());
            image.setSoilMoisturePct(dto.getSoilMoisturePct());
            image.setLightLux(dto.getLightLux());
            image.setIsRaining(dto.getIsRaining());
            image.setOxygenPct(dto.getOxygenPct());
            image.setCo2Ppm(dto.getCo2Ppm());
            image.setPlotId(dto.getPlotId());
            
            // 判断是否异常：只有温度高于舒适温度（35°C）或土壤湿度低于10%才标记为异常
            boolean isAbnormal = false;
            StringBuilder abnormalReason = new StringBuilder();
            // 温度高于舒适温度（高阈值，默认35°C）
            if (dto.getTemperatureC() != null && 
                dto.getTemperatureC().compareTo(new BigDecimal("35")) > 0) {
                isAbnormal = true;
                abnormalReason.append("温度异常");
            }
            // 土壤湿度低于10%
            if (dto.getSoilMoisturePct() != null && 
                dto.getSoilMoisturePct().compareTo(new BigDecimal("10")) < 0) {
                isAbnormal = true;
                if (abnormalReason.length() > 0) abnormalReason.append(", ");
                abnormalReason.append("土壤湿度异常");
            }
            image.setIsAbnormal(isAbnormal);
            image.setAbnormalReason(abnormalReason.length() > 0 ? abnormalReason.toString() : null);
            image.setCreatedAt(new Date());
            
            imageMapper.insert(image);
            log.info("图片记录保存成功 - ID: {}, URL: {}", image.getId(), image.getImageUrl());
            return Result.success(image);
            
        } catch (Exception e) {
            log.error("保存图片记录失败: {}", e.getMessage(), e);
            return Result.error("保存图片记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传并保存图片（一步完成）
     * 上传图片文件，同时保存图片记录和传感器数据
     */
    @PostMapping("/upload-and-save")
    @Transactional
    public Result<Image> uploadAndSave(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "temperatureC", required = false) BigDecimal temperatureC,
            @RequestParam(value = "humidityPct", required = false) BigDecimal humidityPct,
            @RequestParam(value = "soilMoisturePct", required = false) BigDecimal soilMoisturePct,
            @RequestParam(value = "lightLux", required = false) Integer lightLux,
            @RequestParam(value = "isRaining", required = false) Boolean isRaining,
            @RequestParam(value = "oxygenPct", required = false) BigDecimal oxygenPct,
            @RequestParam(value = "co2Ppm", required = false) Integer co2Ppm,
            @RequestParam(value = "plotId", required = false) Integer plotId,
            @RequestParam(value = "recordTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date recordTime) {
        
        try {
            // 1. 上传图片
            if (file.isEmpty()) {
                return Result.error("上传的文件为空");
            }
            
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;
            
            // 保存文件
            Path filePath = uploadDir.resolve(filename);
            Files.write(filePath, file.getBytes());
            
            // 生成访问URL
            String imageUrl = urlPrefix + filename;
            
            // 2. 保存图片记录
            Image image = new Image();
            image.setImageUrl(imageUrl);
            image.setRecordTime(recordTime != null ? recordTime : new Date());
            image.setTemperatureC(temperatureC);
            image.setHumidityPct(humidityPct);
            image.setSoilMoisturePct(soilMoisturePct);
            image.setLightLux(lightLux);
            image.setIsRaining(isRaining != null ? isRaining : false);
            image.setOxygenPct(oxygenPct);
            image.setCo2Ppm(co2Ppm);
            image.setPlotId(plotId);
            
            // 判断是否异常
            boolean isAbnormal = false;
            StringBuilder abnormalReason = new StringBuilder();
            if (temperatureC != null && temperatureC.compareTo(new BigDecimal("35")) > 0) {
                isAbnormal = true;
                abnormalReason.append("温度异常");
            }
            if (soilMoisturePct != null && soilMoisturePct.compareTo(new BigDecimal("10")) < 0) {
                isAbnormal = true;
                if (abnormalReason.length() > 0) abnormalReason.append(", ");
                abnormalReason.append("土壤湿度异常");
            }
            image.setIsAbnormal(isAbnormal);
            image.setAbnormalReason(abnormalReason.length() > 0 ? abnormalReason.toString() : null);
            image.setCreatedAt(new Date());
            
            imageMapper.insert(image);
            
            log.info("图片上传并保存成功 - ID: {}, URL: {}", image.getId(), imageUrl);
            return Result.success(image);
            
        } catch (IOException e) {
            log.error("图片上传失败: {}", e.getMessage(), e);
            return Result.error("图片上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("保存图片记录失败: {}", e.getMessage(), e);
            return Result.error("保存图片记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有图片列表
     */
    @GetMapping
    public Result<List<Image>> getAllImages(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 简单实现：获取所有图片，按创建时间倒序
        List<Image> images = imageMapper.selectList(null);
        // 可以在这里添加分页逻辑
        return Result.success(images);
    }
    
    /**
     * 根据ID删除图片
     */
    @DeleteMapping("/{id}")
    @Transactional
    public Result<String> deleteImage(@PathVariable Long id) {
        try {
            Image image = imageMapper.selectById(id);
            if (image == null) {
                return Result.error("图片不存在");
            }
            
            // 删除文件
            if (image.getImageUrl() != null && image.getImageUrl().startsWith(urlPrefix)) {
                String filename = image.getImageUrl().substring(urlPrefix.length());
                Path filePath = Paths.get(uploadPath, filename);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            }
            
            // 删除数据库记录
            imageMapper.deleteById(id);
            
            log.info("图片删除成功 - ID: {}", id);
            return Result.success("删除成功");
            
        } catch (Exception e) {
            log.error("删除图片失败: {}", e.getMessage(), e);
            return Result.error("删除图片失败: " + e.getMessage());
        }
    }
}
