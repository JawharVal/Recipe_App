package com.example.demo.repositories;

import com.example.demo.model.MealPlan;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    Optional<MealPlan> findByUserAndDate(User user, LocalDate date);
    List<MealPlan> findByUser(User user);
    List<MealPlan> findByUserId(Long userId);
    List<MealPlan> findByRecipes_Id(Long recipeId);


}
