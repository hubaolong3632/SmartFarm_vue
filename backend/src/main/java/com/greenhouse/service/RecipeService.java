package com.greenhouse.service;

import com.greenhouse.dto.RecipeDTO;
import com.greenhouse.entity.Recipe;
import com.greenhouse.mapper.PlotAssignmentMapper;
import com.greenhouse.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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
        Recipe recipe = new Recipe();
        
        // 如果前端提供了 ID，使用前端提供的；否则自动生成
        String recipeId;
        if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
            recipeId = dto.getId().trim();
            Recipe existing = recipeMapper.selectById(recipeId);
            if (existing != null) {
                throw new IllegalArgumentException("配方ID已存在: " + recipeId);
            }
        } else {
            // 自动生成 ID：使用时间戳 + 随机数
            recipeId = "R" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        }
        
        // 确保 ID 被设置
        recipe.setId(recipeId);
        log.debug("创建配方，ID: {}", recipeId);
        
        recipe.setName(dto.getName());
        recipe.setWaterMl(dto.getWaterMl());
        recipe.setNutrientMl(dto.getNutrientMl());
        recipe.setRootingPowderMl(dto.getRootingPowderMl());
        recipe.setSpecialMl(dto.getSpecialMl());
        Date now = new Date();
        recipe.setCreatedAt(now);
        recipe.setUpdatedAt(now);
        
        // 确保 ID 不为空后再插入
        if (recipe.getId() == null || recipe.getId().isEmpty()) {
            throw new IllegalArgumentException("配方ID不能为空");
        }
        
        recipeMapper.insert(recipe);
        log.debug("配方创建成功，ID: {}", recipe.getId());
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
        recipe.setUpdatedAt(new Date());
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

