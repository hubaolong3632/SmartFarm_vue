package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.ImageDTO;
import com.greenhouse.entity.Image;
import com.greenhouse.mapper.ImageMapper;
import com.greenhouse.mapper.PlotMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 图片管理控制器
 */
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ImageController {
    
    private final ImageMapper imageMapper;
    private final PlotMapper plotMapper;
    
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
}
