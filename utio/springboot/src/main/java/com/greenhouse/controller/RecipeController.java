package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.RecipeDTO;
import com.greenhouse.entity.Recipe;
import com.greenhouse.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 配方管理控制器
 */
@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {
    
    private final RecipeService recipeService;
    
    /**
     * 创建配方
     */
    @PostMapping
    public Result<Recipe> create(@Valid @RequestBody RecipeDTO dto) {
        Recipe recipe = recipeService.create(dto);
        return Result.success(recipe);
    }
    
    /**
     * 获取所有配方
     */
    @GetMapping
    public Result<List<Recipe>> getAll() {
        List<Recipe> recipes = recipeService.getAll();
        return Result.success(recipes);
    }
    
    /**
     * 根据ID获取配方
     */
    @GetMapping("/detail")
    public Result<Recipe> getById(@RequestParam String id) {
        Recipe recipe = recipeService.getById(id);
        return Result.success(recipe);
    }
    
    /**
     * 更新配方
     */
    @PutMapping
    public Result<Recipe> update(@RequestParam String id, @Valid @RequestBody RecipeDTO dto) {
        Recipe recipe = recipeService.update(id, dto);
        return Result.success(recipe);
    }
    
    /**
     * 删除配方
     */
    @DeleteMapping
    public Result<Void> delete(@RequestParam String id) {
        recipeService.delete(id);
        return Result.success();
    }
}

