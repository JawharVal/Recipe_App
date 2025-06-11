package com.example.demo.service;


import com.example.demo.dto.RecipeDTO;
import com.example.demo.model.Recipe;


import java.util.List;

public interface RecipeService {
    Recipe createRecipe(RecipeDTO recipeDTO);
    Recipe updateRecipe(Long id, RecipeDTO recipeDTO);
    void deleteRecipe(Long id);
    Recipe getRecipeById(Long id);

    RecipeDTO mapEntityToDTO(Recipe recipe);

    List<Recipe> getAllRecipes();
    RecipeDTO likeRecipe(Long recipeId, String userEmail);
    void deleteRecipesByIdsAndUserEmail(List<Long> recipeIds, String userEmail);

    List<Recipe> searchRecipesByTitle(String title);
    long countRecipes();
    long countTotalLikes();
    List<Recipe> getRecipesByUserEmail(String email);
}
