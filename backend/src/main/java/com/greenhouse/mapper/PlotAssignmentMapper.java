package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.PlotAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 地块配方分配Mapper
 */
@Mapper
public interface PlotAssignmentMapper extends BaseMapper<PlotAssignment> {
    
    /**
     * 查询指定地块的激活分配
     */
    @Select("SELECT * FROM plot_assignments WHERE plot_id = #{plotId} AND is_active = 1 LIMIT 1")
    PlotAssignment findByPlotIdAndIsActiveTrue(@Param("plotId") Integer plotId);
    
    /**
     * 查询指定地块的所有分配记录
     */
    @Select("SELECT * FROM plot_assignments WHERE plot_id = #{plotId} ORDER BY created_at DESC")
    List<PlotAssignment> findByPlotIdOrderByCreatedAtDesc(@Param("plotId") Integer plotId);
    
    /**
     * 查询指定配方的激活分配
     */
    @Select("SELECT * FROM plot_assignments WHERE recipe_id = #{recipeId} AND is_active = 1")
    List<PlotAssignment> findByRecipeIdAndIsActiveTrue(@Param("recipeId") String recipeId);
    
    /**
     * 取消指定地块的所有激活分配
     */
    @Update("UPDATE plot_assignments SET is_active = 0 WHERE plot_id = #{plotId} AND is_active = 1")
    void deactivateByPlotId(@Param("plotId") Integer plotId);
}

