package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.AutomationSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 自动化设置Mapper
 */
@Mapper
public interface AutomationSettingMapper extends BaseMapper<AutomationSetting> {
    
    /**
     * 根据键名查询
     */
    @Select("SELECT * FROM automation_settings WHERE setting_key = #{settingKey}")
    AutomationSetting findBySettingKey(String settingKey);
}

