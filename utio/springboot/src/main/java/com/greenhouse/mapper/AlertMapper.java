package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.Alert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 报警记录Mapper
 */
@Mapper
public interface AlertMapper extends BaseMapper<Alert> {
    
    /**
     * 查询未读报警
     */
    @Select("SELECT * FROM alerts WHERE is_read = 0 ORDER BY created_at DESC")
    List<Alert> findByIsReadFalseOrderByCreatedAtDesc();
    
    /**
     * 查询指定级别的报警
     */
    @Select("SELECT * FROM alerts WHERE level = #{level} ORDER BY created_at DESC")
    List<Alert> findByLevelOrderByCreatedAtDesc(@Param("level") String level);
    
    /**
     * 标记为已读
     */
    @Update("UPDATE alerts SET is_read = 1 WHERE id = #{id}")
    void markAsRead(@Param("id") Long id);
}

