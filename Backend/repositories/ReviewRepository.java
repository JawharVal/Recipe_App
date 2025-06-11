package com.example.demo.repositories;

import com.example.demo.model.Review;
import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRecipe(Recipe recipe);
    List<Review> findByUser(User user);

    Review findByUserAndRecipe(User user, Recipe recipe);
    List<Review> findByRecipeIdOrderByCreatedAtDesc(Long recipeId);

}
