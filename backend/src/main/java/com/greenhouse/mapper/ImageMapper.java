package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.Image;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片Mapper
 */
@Mapper
public interface ImageMapper extends BaseMapper<Image> {
    
    /**
     * 查询指定日期范围的图片
     */
    @Select("SELECT * FROM images WHERE record_time >= #{startTime} AND record_time <= #{endTime} ORDER BY record_time DESC")
    List<Image> findByRecordTimeBetweenOrderByRecordTimeDesc(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定日期的图片
     */
    @Select("SELECT * FROM images WHERE DATE(record_time) = #{date} ORDER BY record_time DESC")
    List<Image> findByDate(@Param("date") LocalDate date);
    
    /**
     * 查询异常图片
     */
    @Select("SELECT * FROM images WHERE is_abnormal = 1 ORDER BY record_time DESC")
    List<Image> findByIsAbnormalTrueOrderByRecordTimeDesc();
    
    /**
     * 查询指定地块的图片
     */
    @Select("SELECT * FROM images WHERE plot_id = #{plotId} ORDER BY record_time DESC")
    List<Image> findByPlotIdOrderByRecordTimeDesc(@Param("plotId") Integer plotId);
}

