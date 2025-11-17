package com.greenhouse.util;

import com.greenhouse.dto.ImageDTO;
import com.greenhouse.entity.Image;
import com.greenhouse.mapper.ImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 图片数据插入工具类
 * 用于向数据库插入基本图片数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageDataInserter {
    
    private final ImageMapper imageMapper;
    
    /**
     * 插入基本图片数据
     */
    public void insertSampleImages() {
        List<Image> images = new ArrayList<>();
        Date now = new Date();
        
        // 正常情况的图片（地块1）
        images.add(createImage("/api/images/files/sample_001.jpg", 
            new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000L), 
            25.5, 60.2, 45.2, 12000, false, 20.5, 400, 1, false, null));
        images.add(createImage("/api/images/files/sample_002.jpg", 
            new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000L), 
            26.2, 59.8, 44.8, 15000, false, 20.6, 410, 1, false, null));
        images.add(createImage("/api/images/files/sample_003.jpg", 
            new Date(now.getTime() - 1 * 24 * 60 * 60 * 1000L), 
            24.8, 61.5, 46.5, 10000, false, 20.4, 390, 1, false, null));
        
        // 正常情况的图片（地块2）
        images.add(createImage("/api/images/files/sample_004.jpg", 
            new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000L), 
            27.1, 58.5, 43.5, 18000, false, 20.7, 420, 2, false, null));
        images.add(createImage("/api/images/files/sample_005.jpg", 
            new Date(now.getTime() - 1 * 24 * 60 * 60 * 1000L), 
            26.5, 59.2, 44.2, 16000, false, 20.6, 415, 2, false, null));
        
        // 正常情况的图片（地块3）
        images.add(createImage("/api/images/files/sample_006.jpg", 
            new Date(now.getTime() - 1 * 24 * 60 * 60 * 1000L), 
            28.5, 57.2, 42.2, 22000, false, 20.8, 430, 3, false, null));
        images.add(createImage("/api/images/files/sample_007.jpg", 
            new Date(now.getTime() - 12 * 60 * 60 * 1000L), 
            27.8, 58.0, 43.0, 20000, false, 20.7, 425, 3, false, null));
        
        // 正常情况的图片（地块4）
        images.add(createImage("/api/images/files/sample_008.jpg", 
            new Date(now.getTime() - 1 * 24 * 60 * 60 * 1000L), 
            29.2, 56.8, 41.8, 25000, false, 20.9, 440, 4, false, null));
        images.add(createImage("/api/images/files/sample_009.jpg", 
            new Date(now.getTime() - 12 * 60 * 60 * 1000L), 
            28.5, 57.5, 42.5, 23000, false, 20.8, 435, 4, false, null));
        
        // 异常情况：温度过高（>35°C）
        images.add(createImage("/api/images/files/sample_010.jpg", 
            new Date(now.getTime() - 3 * 60 * 60 * 1000L), 
            36.5, 75.0, 35.0, 20000, false, 19.5, 1100, 1, true, "温度异常"));
        images.add(createImage("/api/images/files/sample_011.jpg", 
            new Date(now.getTime() - 2 * 60 * 60 * 1000L), 
            38.2, 78.5, 32.5, 22000, false, 19.2, 1150, 2, true, "温度异常"));
        
        // 异常情况：土壤湿度过低（<10%）
        images.add(createImage("/api/images/files/sample_012.jpg", 
            new Date(now.getTime() - 4 * 60 * 60 * 1000L), 
            28.5, 55.0, 8.5, 18000, false, 21.0, 500, 1, true, "土壤湿度异常"));
        images.add(createImage("/api/images/files/sample_013.jpg", 
            new Date(now.getTime() - 3 * 60 * 60 * 1000L), 
            29.2, 54.5, 7.2, 19000, false, 21.1, 510, 2, true, "土壤湿度异常"));
        
        // 异常情况：温度过高且土壤湿度过低
        images.add(createImage("/api/images/files/sample_014.jpg", 
            new Date(now.getTime() - 5 * 60 * 60 * 1000L), 
            40.5, 82.0, 5.5, 25000, false, 18.5, 1200, 1, true, "温度异常, 土壤湿度异常"));
        
        // 下雨情况的图片
        images.add(createImage("/api/images/files/sample_015.jpg", 
            new Date(now.getTime() - 6 * 60 * 60 * 1000L), 
            22.5, 75.0, 50.0, 5000, true, 21.5, 450, 1, false, null));
        images.add(createImage("/api/images/files/sample_016.jpg", 
            new Date(now.getTime() - 5 * 60 * 60 * 1000L), 
            21.8, 78.5, 52.5, 3000, true, 21.6, 460, 2, false, null));
        
        // 低光照情况的图片
        images.add(createImage("/api/images/files/sample_017.jpg", 
            new Date(now.getTime() - 8 * 60 * 60 * 1000L), 
            19.5, 48.8, 38.8, 2000, false, 22.5, 600, 1, false, null));
        images.add(createImage("/api/images/files/sample_018.jpg", 
            new Date(now.getTime() - 7 * 60 * 60 * 1000L), 
            18.2, 47.5, 37.5, 500, true, 22.6, 610, 2, false, null));
        
        // 今天的图片（正常情况）
        images.add(createImage("/api/images/files/sample_019.jpg", 
            new Date(now.getTime() - 3 * 60 * 60 * 1000L), 
            25.0, 60.0, 45.0, 12000, false, 20.5, 400, 1, false, null));
        images.add(createImage("/api/images/files/sample_020.jpg", 
            new Date(now.getTime() - 2 * 60 * 60 * 1000L), 
            24.5, 61.0, 46.0, 11000, false, 20.6, 410, 2, false, null));
        images.add(createImage("/api/images/files/sample_021.jpg", 
            new Date(now.getTime() - 1 * 60 * 60 * 1000L), 
            26.0, 59.5, 44.5, 13000, false, 20.4, 390, 3, false, null));
        images.add(createImage("/api/images/files/sample_022.jpg", 
            now, 
            25.5, 60.5, 45.5, 12500, false, 20.5, 405, 4, false, null));
        
        // 批量插入
        for (Image image : images) {
            try {
                imageMapper.insert(image);
                log.info("插入图片数据成功: {}", image.getImageUrl());
            } catch (Exception e) {
                log.error("插入图片数据失败: {} - {}", image.getImageUrl(), e.getMessage());
            }
        }
        
        log.info("图片数据插入完成，共插入 {} 条记录", images.size());
    }
    
    /**
     * 创建图片对象
     */
    private Image createImage(String imageUrl, Date recordTime, 
                            double temperatureC, double humidityPct, double soilMoisturePct,
                            int lightLux, boolean isRaining, double oxygenPct, int co2Ppm,
                            Integer plotId, boolean isAbnormal, String abnormalReason) {
        Image image = new Image();
        image.setImageUrl(imageUrl);
        image.setRecordTime(recordTime);
        image.setTemperatureC(BigDecimal.valueOf(temperatureC));
        image.setHumidityPct(BigDecimal.valueOf(humidityPct));
        image.setSoilMoisturePct(BigDecimal.valueOf(soilMoisturePct));
        image.setLightLux(lightLux);
        image.setIsRaining(isRaining);
        image.setOxygenPct(BigDecimal.valueOf(oxygenPct));
        image.setCo2Ppm(co2Ppm);
        image.setPlotId(plotId);
        image.setIsAbnormal(isAbnormal);
        image.setAbnormalReason(abnormalReason);
        image.setCreatedAt(new Date());
        return image;
    }
}

