package com.greenhouse.repository;

import com.greenhouse.entity.PlotAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 地块配方分配Repository
 */
@Repository
public interface PlotAssignmentRepository extends JpaRepository<PlotAssignment, Long> {
    
    /**
     * 查询指定地块的激活分配
     */
    Optional<PlotAssignment> findByPlotIdAndIsActiveTrue(Integer plotId);
    
    /**
     * 查询指定地块的所有分配记录
     */
    List<PlotAssignment> findByPlotIdOrderByCreatedAtDesc(Integer plotId);
    
    /**
     * 查询指定配方的激活分配
     */
    List<PlotAssignment> findByRecipeIdAndIsActiveTrue(String recipeId);
    
    /**
     * 取消指定地块的所有激活分配
     */
    @Modifying
    @Query("UPDATE PlotAssignment p SET p.isActive = false WHERE p.plot.id = :plotId AND p.isActive = true")
    void deactivateByPlotId(@Param("plotId") Integer plotId);
}

