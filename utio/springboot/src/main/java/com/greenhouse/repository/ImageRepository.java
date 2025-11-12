package com.greenhouse.repository;

import com.greenhouse.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片Repository
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    /**
     * 查询指定日期范围的图片
     */
    List<Image> findByRecordTimeBetweenOrderByRecordTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询指定日期的图片
     */
    @Query("SELECT i FROM Image i WHERE DATE(i.recordTime) = :date ORDER BY i.recordTime DESC")
    List<Image> findByDate(@Param("date") LocalDate date);
    
    /**
     * 查询异常图片
     */
    List<Image> findByIsAbnormalTrueOrderByRecordTimeDesc();
    
    /**
     * 查询指定地块的图片
     */
    List<Image> findByPlotIdOrderByRecordTimeDesc(Integer plotId);
}

