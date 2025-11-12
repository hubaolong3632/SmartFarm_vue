package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配方Mapper
 */
@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {
}

