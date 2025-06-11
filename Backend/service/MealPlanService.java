package com.example.demo.service;

import com.example.demo.dto.MealPlanDTO;
import com.example.demo.dto.NoteDTO;

import java.time.LocalDate;
import java.util.List;

public interface MealPlanService {
    MealPlanDTO getMealPlanForDate(String userEmail, LocalDate date);
    List<MealPlanDTO> getAllMealPlans(String userEmail);
    void addRecipeToMealPlan(String userEmail, LocalDate date, Long recipeId);
    void removeRecipeFromMealPlan(String userEmail, LocalDate date, Long recipeId);
    NoteDTO addNoteToMealPlan(String userEmail, LocalDate date, NoteDTO noteDTO);
    void deleteNoteFromMealPlan(String userEmail, LocalDate date, Long noteId);
}
