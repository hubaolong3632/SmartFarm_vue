package com.greenhouse.repository;

import com.greenhouse.entity.AutomationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 自动化设置Repository
 */
@Repository
public interface AutomationSettingRepository extends JpaRepository<AutomationSetting, Integer> {
    
    /**
     * 根据键名查询
     */
    Optional<AutomationSetting> findBySettingKey(String settingKey);
}

