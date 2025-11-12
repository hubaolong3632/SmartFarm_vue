package com.greenhouse.repository;

import com.greenhouse.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 配方Repository
 */
@Repository
public interface RecipeRepository extends JpaRepository<Recipe, String> {
}

