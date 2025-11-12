package com.greenhouse.service;

import com.greenhouse.dto.RecipeDTO;
import com.greenhouse.entity.Recipe;
import com.greenhouse.mapper.PlotAssignmentMapper;
import com.greenhouse.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配方服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {
    
    private final RecipeMapper recipeMapper;
    private final PlotAssignmentMapper plotAssignmentMapper;
    
    /**
     * 创建配方
     */
    @Transactional
    public Recipe create(RecipeDTO dto) {
        Recipe existing = recipeMapper.selectById(dto.getId());
        if (existing != null) {
            throw new IllegalArgumentException("配方ID已存在: " + dto.getId());
        }
        Recipe recipe = new Recipe();
        recipe.setId(dto.getId());
        recipe.setName(dto.getName());
        recipe.setWaterMl(dto.getWaterMl());
        recipe.setNutrientMl(dto.getNutrientMl());
        recipe.setRootingPowderMl(dto.getRootingPowderMl());
        recipe.setSpecialMl(dto.getSpecialMl());
        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());
        recipeMapper.insert(recipe);
        return recipe;
    }
    
    /**
     * 获取所有配方
     */
    public List<Recipe> getAll() {
        return recipeMapper.selectList(null);
    }
    
    /**
     * 根据ID获取配方
     */
    public Recipe getById(String id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new IllegalArgumentException("配方不存在: " + id);
        }
        return recipe;
    }
    
    /**
     * 更新配方
     */
    @Transactional
    public Recipe update(String id, RecipeDTO dto) {
        Recipe recipe = getById(id);
        recipe.setName(dto.getName());
        recipe.setWaterMl(dto.getWaterMl());
        recipe.setNutrientMl(dto.getNutrientMl());
        recipe.setRootingPowderMl(dto.getRootingPowderMl());
        recipe.setSpecialMl(dto.getSpecialMl());
        recipe.setUpdatedAt(LocalDateTime.now());
        recipeMapper.updateById(recipe);
        return recipe;
    }
    
    /**
     * 删除配方
     */
    @Transactional
    public void delete(String id) {
        // 检查是否有地块正在使用此配方
        List<com.greenhouse.entity.PlotAssignment> assignments = plotAssignmentMapper.findByRecipeIdAndIsActiveTrue(id);
        if (!assignments.isEmpty()) {
            throw new IllegalArgumentException("该配方正在被使用，无法删除");
        }
        recipeMapper.deleteById(id);
    }
}

