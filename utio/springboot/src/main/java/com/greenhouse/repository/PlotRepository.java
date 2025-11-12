package com.greenhouse.repository;

import com.greenhouse.entity.Plot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 地块Repository
 */
@Repository
public interface PlotRepository extends JpaRepository<Plot, Integer> {
    
    /**
     * 根据地块编号查询
     */
    Optional<Plot> findByPlotNumber(Integer plotNumber);
}

