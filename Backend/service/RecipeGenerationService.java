package com.example.demo.service;


import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import com.example.demo.repositories.RecipeRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RecipeGenerationService {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    public Recipe generateRecipeWithLimitCheck(User user, String ingredients) {
        // Check if the current cycle should be reset:
        LocalDateTime cycleStart = user.getRecipeGenerationCycleStart();
        if (cycleStart == null || LocalDateTime.now().isAfter(cycleStart.plusMonths(1))) {
            // Reset counter and update cycle start:
            user.setRecipeGenerationCount(0);
            user.setRecipeGenerationCycleStart(LocalDateTime.now());
            userRepository.save(user);
        }

        // Determine the allowed limit based on subscription type:
        int limit;
        switch (user.getSubscriptionType()) {
            case FREE:
                limit = 3;
                break;
            case PLUS:
                limit = 10;
                break;
            case PRO:
                limit = Integer.MAX_VALUE;
                break;
            default:
                limit = 3;
        }

        // Check if the user has reached the monthly limit:
        if (user.getRecipeGenerationCount() >= limit) {
            throw new SubscriptionLimitException("Monthly recipe generation limit reached for your subscription tier.");
        }

        // Call your existing logic to generate a recipe (or call an external AI service):
        Recipe generatedRecipe = generateRecipeFromIngredients(ingredients);

        // Increment the count and update the user:
        user.setRecipeGenerationCount(user.getRecipeGenerationCount() + 1);
        userRepository.save(user);

        // Save and return the generated recipe:
        return recipeRepository.save(generatedRecipe);
    }

    private Recipe generateRecipeFromIngredients(String ingredients) {
        // Your existing logic to generate a Recipe from the given ingredients.
        Recipe recipe = new Recipe();
        recipe.setTitle("Generated Recipe for " + ingredients);
        recipe.setIngredients(ingredients);
        // Set other fields as needed...
        return recipe;
    }
}
