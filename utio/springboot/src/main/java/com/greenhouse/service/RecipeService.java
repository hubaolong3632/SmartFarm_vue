package com.greenhouse.service;

import com.greenhouse.dto.RecipeDTO;
import com.greenhouse.entity.Recipe;
import com.greenhouse.repository.PlotAssignmentRepository;
import com.greenhouse.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 配方服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {
    
    private final RecipeRepository recipeRepository;
    private final PlotAssignmentRepository plotAssignmentRepository;
    
    /**
     * 创建配方
     */
    @Transactional
    public Recipe create(RecipeDTO dto) {
        if (recipeRepository.existsById(dto.getId())) {
            throw new IllegalArgumentException("配方ID已存在: " + dto.getId());
        }
        Recipe recipe = new Recipe();
        recipe.setId(dto.getId());
        recipe.setName(dto.getName());
        recipe.setWaterMl(dto.getWaterMl());
        recipe.setNutrientMl(dto.getNutrientMl());
        recipe.setRootingPowderMl(dto.getRootingPowderMl());
        recipe.setSpecialMl(dto.getSpecialMl());
        return recipeRepository.save(recipe);
    }
    
    /**
     * 获取所有配方
     */
    public List<Recipe> getAll() {
        return recipeRepository.findAll();
    }
    
    /**
     * 根据ID获取配方
     */
    public Recipe getById(String id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("配方不存在: " + id));
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
        return recipeRepository.save(recipe);
    }
    
    /**
     * 删除配方
     */
    @Transactional
    public void delete(String id) {
        // 检查是否有地块正在使用此配方
        long activeCount = plotAssignmentRepository.findByRecipeIdAndIsActiveTrue(id).size();
        if (activeCount > 0) {
            throw new IllegalArgumentException("该配方正在被使用，无法删除");
        }
        recipeRepository.deleteById(id);
    }
}

