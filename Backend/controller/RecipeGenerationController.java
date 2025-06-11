package com.example.demo.controller;


import com.example.demo.dto.RecipeDTO;

import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import com.example.demo.service.RecipeGenerationService;
import com.example.demo.service.SubscriptionLimitException;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipe-generation")
public class RecipeGenerationController {

    @Autowired
    private RecipeGenerationService recipeGenerationService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> generateRecipe(@RequestBody String ingredients, Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userService.getUserEntityByEmail(userEmail);
        try {
            Recipe recipe = recipeGenerationService.generateRecipeWithLimitCheck(user, ingredients);
            RecipeDTO dto = mapToDto(recipe);
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        } catch (SubscriptionLimitException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Generation Limit Exceeded", ex.getMessage()));
        }
    }

    private RecipeDTO mapToDto(Recipe recipe) {
        RecipeDTO dto = new RecipeDTO();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setIngredients(recipe.getIngredients());
        return dto;
    }

    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
