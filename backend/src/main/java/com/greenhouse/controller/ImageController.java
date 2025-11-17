package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.ImageDTO;
import com.greenhouse.entity.Image;
import com.greenhouse.entity.Plot;
import com.greenhouse.entity.SensorData;
import com.greenhouse.mapper.ImageMapper;
import com.greenhouse.mapper.PlotMapper;
import com.greenhouse.service.SensorDataService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final SensorDataService sensorDataService;
    
    @Value("${file.upload.path:./uploads/images}")
    private String uploadPath;
    
    @Value("${file.upload.url-prefix:/api/images/files/}")
    private String urlPrefix;
    
    /**
     * 创建图片记录
     * 只需要上传 url 字段，其他传感器数据从最新的实时传感器数据中获取
     */




    /**
     * 上传文件并自动保存图片记录
     * 上传成功后自动获取最新传感器数据并保存到数据库
     * @param file 上传的文件
     * @param request HTTP请求对象
     * @return 保存的图片记录
     */
    @PostMapping("/file")
    @Transactional
    public Result<Image> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.error("上传的文件为空");
        }
        
        try {
            // 1. 处理文件名（添加时间戳防止重复）
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || !originalFileName.contains(".")) {
                return Result.error("文件名格式不正确");
            }
            
            long currentTimeMillis = System.currentTimeMillis();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = currentTimeMillis + fileExtension;
            
            // 2. 配置文件存储路径
            String basePath = System.getProperty("user.dir") + "/file/1/";
            File storageDir = new File(basePath);
            if (!storageDir.exists()) {
                storageDir.mkdirs(); // 递归创建目录
            }

            // 3. 保存文件到服务器
            Path targetPath = Paths.get(basePath + fileName);
            Files.copy(file.getInputStream(), targetPath);

            // 4. 构建可访问的URL
            String serverUrl = request.getScheme() + "://" + request.getServerName()
                    + ":" + request.getServerPort();
            // 获取 context-path（如果有配置的话，如 /api）
            String contextPath = request.getContextPath();
            // 构建完整的文件访问URL
            String fileContextPath = contextPath + "/file/1/";
            String fileAccessUrl = serverUrl + fileContextPath + fileName;

            log.info("文件上传成功: {} -> {}", originalFileName, fileAccessUrl);

            // 5. 自动保存图片记录（获取最新传感器数据并合并）
            ImageDTO dto = new ImageDTO();
            dto.setUrl(fileAccessUrl);
            
            // 调用 create 方法保存图片记录
            Result<Image> createResult = create(dto);
            
            if (createResult.getCode() != null && createResult.getCode() == 200) {
                log.info("图片记录保存成功 - ID: {}", createResult.getData() != null ? createResult.getData().getId() : "未知");
                return createResult;
            } else {
                log.warn("图片记录保存失败: {}", createResult.getMessage());
                return Result.error("文件上传成功，但保存图片记录失败: " + createResult.getMessage());
            }

        } catch (IOException e) {
            log.error("上传文件失败: {}", e.getMessage(), e);
            return Result.error("上传文件失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理文件上传时发生错误: {}", e.getMessage(), e);
            return Result.error("处理文件上传时发生错误: " + e.getMessage());
        }
    }









    @PostMapping
    @Transactional
    public Result<Image> create(@RequestBody ImageDTO dto) {
        // 验证 url 是否提供
        if (dto.getUrl() == null || dto.getUrl().trim().isEmpty()) {
            return Result.error("图片URL不能为空");
        }

        // 获取最新的传感器数据
        SensorData latestSensorData = sensorDataService.getLatest();

        Image image = new Image();
        image.setImageUrl(dto.getUrl());

        // 记录时间：使用当前时间
        image.setRecordTime(new Date());

        // 从最新传感器数据中获取所有传感器字段
        if (latestSensorData != null) {
            image.setTemperatureC(latestSensorData.getTemperatureC());
            image.setHumidityPct(latestSensorData.getHumidityPct());
            image.setSoilMoisturePct(latestSensorData.getSoilMoisturePct());
            image.setLightLux(latestSensorData.getLightLux());
            image.setIsRaining(latestSensorData.getIsRaining());
            image.setOxygenPct(latestSensorData.getOxygenPct());
            image.setCo2Ppm(latestSensorData.getCo2Ppm());
            log.info("从最新传感器数据获取数据: 温度={}, 湿度={}, 土壤湿度={}, 光照={}, 是否下雨={}, 氧气={}, 二氧化碳={}",
                latestSensorData.getTemperatureC(),
                latestSensorData.getHumidityPct(),
                latestSensorData.getSoilMoisturePct(),
                latestSensorData.getLightLux(),
                latestSensorData.getIsRaining(),
                latestSensorData.getOxygenPct(),
                latestSensorData.getCo2Ppm());
        } else {
            // 如果没有传感器数据，设置默认值
            log.warn("未找到最新传感器数据，使用默认值");
            image.setTemperatureC(BigDecimal.ZERO);
            image.setHumidityPct(BigDecimal.ZERO);
            image.setSoilMoisturePct(BigDecimal.ZERO);
            image.setLightLux(0);
            image.setIsRaining(false);
            image.setOxygenPct(BigDecimal.ZERO);
            image.setCo2Ppm(0);
        }

        // 验证 plotId 是否存在，如果不存在则设置为 null
        if (dto.getPlotId() != null) {
            Plot plot = plotMapper.selectById(dto.getPlotId());
            if (plot == null) {
                log.warn("地块ID {} 不存在，将 plotId 设置为 null", dto.getPlotId());
                image.setPlotId(null);
            } else {
                image.setPlotId(dto.getPlotId());
            }
        } else {
            image.setPlotId(null);
        }

        // 判断是否异常
        checkAndSetAbnormalStatus(image);
        image.setCreatedAt(new Date());

        imageMapper.insert(image);
        log.info("图片记录创建成功 - ID: {}, URL: {}", image.getId(), image.getImageUrl());
        return Result.success(image);
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
            
            // 验证 plotId 是否存在，如果不存在则设置为 null
            if (dto.getPlotId() != null) {
                Plot plot = plotMapper.selectById(dto.getPlotId());
                if (plot == null) {
                    log.warn("地块ID {} 不存在，将 plotId 设置为 null", dto.getPlotId());
                    image.setPlotId(null);
                } else {
                    image.setPlotId(dto.getPlotId());
                }
            } else {
                image.setPlotId(null);
            }
            
            // 判断是否异常
            checkAndSetAbnormalStatus(image);
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
     * 获取所有图片列表
     */
    @GetMapping
    public Result<List<Image>> getAllImages(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "100") Integer size) {
        // 获取所有图片，按记录时间倒序
        List<Image> images = imageMapper.selectList(null);
        // 按记录时间倒序排序
        images.sort((a, b) -> {
            if (a.getRecordTime() == null || b.getRecordTime() == null) {
                return 0;
            }
            return b.getRecordTime().compareTo(a.getRecordTime());
        });
        // 简单分页（可以后续优化为数据库分页）
        int start = (page - 1) * size;
        int end = Math.min(start + size, images.size());
        if (start >= images.size()) {
            return Result.success(new java.util.ArrayList<>());
        }
        return Result.success(images.subList(start, end));
    }
    
    /**
     * 根据ID删除图片
     */
    @DeleteMapping
    @Transactional
    public Result<String> deleteImage(@RequestParam Long id) {
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
    
    /**
     * 检查并设置图片的异常状态
     * 根据传感器数据判断是否存在异常情况
     * 
     * @param image 图片对象
     */
    private void checkAndSetAbnormalStatus(Image image) {
        boolean isAbnormal = false;
        StringBuilder abnormalReason = new StringBuilder();
        
        // 1. 温度异常判断
        if (image.getTemperatureC() != null) {
            BigDecimal temp = image.getTemperatureC();
            // 温度过低（低于10°C）
            if (temp.compareTo(new BigDecimal("10")) < 0) {
                isAbnormal = true;
                abnormalReason.append("温度过低");
            }
            // 温度过高（高于35°C）
            else if (temp.compareTo(new BigDecimal("35")) > 0) {
                isAbnormal = true;
                if (abnormalReason.length() > 0) abnormalReason.append(", ");
                abnormalReason.append("温度过高");
            }
        }
        
        // 2. 土壤湿度异常判断
        if (image.getSoilMoisturePct() != null) {
            BigDecimal soilMoisture = image.getSoilMoisturePct();
            // 土壤湿度过低（低于10%）
            if (soilMoisture.compareTo(new BigDecimal("10")) < 0) {
                isAbnormal = true;
                if (abnormalReason.length() > 0) abnormalReason.append(", ");
                abnormalReason.append("土壤湿度过低");
            }
            // 土壤湿度过高（高于90%）
            else if (soilMoisture.compareTo(new BigDecimal("90")) > 0) {
                isAbnormal = true;
                if (abnormalReason.length() > 0) abnormalReason.append(", ");
                abnormalReason.append("土壤湿度过高");
            }
        }
        
        // 3. 空气湿度异常判断
        if (image.getHumidityPct() != null) {
            BigDecimal humidity = image.getHumidityPct();
            // 湿度过低（低于30%）
            if (humidity.compareTo(new BigDecimal("30")) < 0) {
                isAbnormal = true;
                if (abnormalReason.length() > 0) abnormalReason.append(", ");
                abnormalReason.append("空气湿度过低");
            }
            // 湿度过高（高于90%）
            else if (humidity.compareTo(new BigDecimal("90")) > 0) {
                isAbnormal = true;
                if (abnormalReason.length() > 0) abnormalReason.append(", ");
                abnormalReason.append("空气湿度过高");
            }
        }
        
        // 4. 氧气异常判断
        if (image.getOxygenPct() != null) {
            BigDecimal oxygen = image.getOxygenPct();
            // 氧气含量过低（低于18%）
            if (oxygen.compareTo(new BigDecimal("18")) < 0) {
                isAbnormal = true;
                if (abnormalReason.length() > 0) abnormalReason.append(", ");
                abnormalReason.append("氧气含量过低");
            }
        }
        
        // 设置异常状态和原因
        image.setIsAbnormal(isAbnormal);
        image.setAbnormalReason(abnormalReason.length() > 0 ? abnormalReason.toString() : null);
        
        if (isAbnormal) {
            log.warn("检测到异常情况: {}", abnormalReason.toString());
        }
    }
}
