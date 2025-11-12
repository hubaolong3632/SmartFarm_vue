package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.ImageDTO;
import com.greenhouse.entity.Image;
import com.greenhouse.entity.Plot;
import com.greenhouse.repository.ImageRepository;
import com.greenhouse.repository.PlotRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片管理控制器
 */
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {
    
    private final ImageRepository imageRepository;
    private final PlotRepository plotRepository;
    
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
        image.setSoilMoisturePct(dto.getSoilMoisturePct());
        image.setLightLux(dto.getLightLux());
        if (dto.getPlotId() != null) {
            Plot plot = plotRepository.findById(dto.getPlotId())
                    .orElse(null);
            image.setPlot(plot);
        }
        // 判断是否异常
        boolean isAbnormal = false;
        String abnormalReason = null;
        if (dto.getTemperatureC() != null && 
            (dto.getTemperatureC().compareTo(new BigDecimal("10")) < 0 || 
             dto.getTemperatureC().compareTo(new BigDecimal("35")) > 0)) {
            isAbnormal = true;
            abnormalReason = "温度异常";
        }
        if (dto.getSoilMoisturePct() != null && 
            dto.getSoilMoisturePct().compareTo(new BigDecimal("35")) < 0) {
            isAbnormal = true;
            abnormalReason = abnormalReason == null ? "土壤湿度异常" : abnormalReason + ", 土壤湿度异常";
        }
        image.setIsAbnormal(isAbnormal);
        image.setAbnormalReason(abnormalReason);
        
        return Result.success(imageRepository.save(image));
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
            image.setSoilMoisturePct(dto.getSoilMoisturePct());
            image.setLightLux(dto.getLightLux());
            if (dto.getPlotId() != null) {
                Plot plot = plotRepository.findById(dto.getPlotId()).orElse(null);
                image.setPlot(plot);
            }
            // 判断是否异常
            boolean isAbnormal = false;
            String abnormalReason = null;
            if (dto.getTemperatureC() != null && 
                (dto.getTemperatureC().compareTo(new BigDecimal("10")) < 0 || 
                 dto.getTemperatureC().compareTo(new BigDecimal("35")) > 0)) {
                isAbnormal = true;
                abnormalReason = "温度异常";
            }
            if (dto.getSoilMoisturePct() != null && 
                dto.getSoilMoisturePct().compareTo(new BigDecimal("35")) < 0) {
                isAbnormal = true;
                abnormalReason = abnormalReason == null ? "土壤湿度异常" : abnormalReason + ", 土壤湿度异常";
            }
            image.setIsAbnormal(isAbnormal);
            image.setAbnormalReason(abnormalReason);
            return image;
        }).toList();
        return Result.success(imageRepository.saveAll(images));
    }
    
    /**
     * 根据日期查询图片
     */
    @GetMapping("/date/{date}")
    public Result<List<Image>> getByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        List<Image> images = imageRepository.findByDate(date);
        return Result.success(images);
    }
    
    /**
     * 获取异常图片
     */
    @GetMapping("/abnormal")
    public Result<List<Image>> getAbnormal() {
        return Result.success(imageRepository.findByIsAbnormalTrueOrderByRecordTimeDesc());
    }
    
    /**
     * 获取指定地块的图片
     */
    @GetMapping("/plot/{plotId}")
    public Result<List<Image>> getByPlotId(@PathVariable Integer plotId) {
        return Result.success(imageRepository.findByPlotIdOrderByRecordTimeDesc(plotId));
    }
}

