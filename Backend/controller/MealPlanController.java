package com.example.demo.controller;

import com.example.demo.dto.MealPlanDTO;
import com.example.demo.dto.NoteDTO;
import com.example.demo.service.MealPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/mealplans")
public class MealPlanController {

    @Autowired
    private MealPlanService mealPlanService;

    @GetMapping
    public ResponseEntity<List<MealPlanDTO>> getAllMealPlans(Authentication authentication) {
        String userEmail = authentication.getName();
        List<MealPlanDTO> mealPlans = mealPlanService.getAllMealPlans(userEmail);
        return ResponseEntity.ok(mealPlans);
    }

    @GetMapping("/{date}")
    public ResponseEntity<MealPlanDTO> getMealPlanForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        MealPlanDTO mealPlan = mealPlanService.getMealPlanForDate(userEmail, date);
        return ResponseEntity.ok(mealPlan);
    }

    @PostMapping("/{date}/recipes/{recipeId}")
    public ResponseEntity<Void> addRecipeToMealPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long recipeId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        mealPlanService.addRecipeToMealPlan(userEmail, date, recipeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{date}/recipes/{recipeId}")
    public ResponseEntity<Void> removeRecipeFromMealPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long recipeId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        mealPlanService.removeRecipeFromMealPlan(userEmail, date, recipeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{date}/notes")
    public ResponseEntity<NoteDTO> addNoteToMealPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody NoteDTO noteDTO,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        NoteDTO createdNote = mealPlanService.addNoteToMealPlan(userEmail, date, noteDTO);
        return ResponseEntity.ok(createdNote);
    }

    @DeleteMapping("/{date}/notes/{noteId}")
    public ResponseEntity<Void> deleteNoteFromMealPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long noteId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        mealPlanService.deleteNoteFromMealPlan(userEmail, date, noteId);
        return ResponseEntity.ok().build();
    }
}
