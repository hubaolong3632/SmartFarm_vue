package com.greenhouse.repository;

import com.greenhouse.entity.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 传感器数据Repository
 */
@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
    
    /**
     * 查询指定时间范围的数据
     */
    List<SensorData> findByRecordTimeBetweenOrderByRecordTimeAsc(
            LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询最近24小时的数据
     */
    @Query("SELECT s FROM SensorData s WHERE s.recordTime >= :startTime AND s.recordTime <= :endTime ORDER BY s.recordTime ASC")
    List<SensorData> findLast24Hours(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取最新的一条记录
     */
    Optional<SensorData> findFirstByOrderByRecordTimeDesc();
    
    /**
     * 删除指定时间之前的数据
     */
    void deleteByRecordTimeBefore(LocalDateTime beforeTime);
}

