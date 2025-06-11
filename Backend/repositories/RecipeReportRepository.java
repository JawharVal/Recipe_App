package com.example.demo.repositories;

import com.example.demo.model.Recipe;
import com.example.demo.model.RecipeReport;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipeReportRepository extends JpaRepository<RecipeReport, Long> {
    Optional<RecipeReport> findByRecipeAndReporter(Recipe recipe, User reporter);
    void deleteAllByReporter(User reporter);

}
