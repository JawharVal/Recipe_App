package com.example.demo.repositories;

import com.example.demo.model.Challenge;
import com.example.demo.model.Recipe;
import com.example.demo.model.RecipeSubmission;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeSubmissionRepository extends JpaRepository<RecipeSubmission, Long> {
    boolean existsByUserAndChallenge(User user, Challenge challenge);
    boolean existsByRecipeAndUserAndChallengeNot(Recipe recipe, User user, Challenge challenge);
    long countByChallengeAndUser(Challenge challenge, User user);


    boolean existsByChallengeAndRecipe(Challenge challenge, Recipe recipe);
    long countByChallengeAndUserAndRecipe_IsAiGenerated(Challenge challenge, User user, Boolean isAiGenerated);

    List<RecipeSubmission> findByUser(User user);
    List<RecipeSubmission> findByRecipe_Id(Long recipeId);

    void deleteByChallenge(Challenge challenge);

}
