package com.example.demo.controller;

import com.example.demo.dto.MealPlanDTO;
import com.example.demo.dto.NoteDTO;
import com.example.demo.service.MealPlanService;
import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users/{userId}/mealplans")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminMealPlanController {

    @Autowired
    private MealPlanService mealPlanService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<MealPlanDTO>> getAllMealPlans(@PathVariable Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));


        String userEmail = user.getEmail();

        List<MealPlanDTO> mealPlans = mealPlanService.getAllMealPlans(userEmail);
        return ResponseEntity.ok(mealPlans);
    }

    @GetMapping("/{date}")
    public ResponseEntity<MealPlanDTO> getMealPlanForDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String userEmail = user.getEmail();

        MealPlanDTO mealPlan = mealPlanService.getMealPlanForDate(userEmail, date);
        return ResponseEntity.ok(mealPlan);
    }

    @PostMapping("/{date}/recipes/{recipeId}")
    public ResponseEntity<Void> addRecipeToMealPlan(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long recipeId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        mealPlanService.addRecipeToMealPlan(user.getEmail(), date, recipeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{date}/recipes/{recipeId}")
    public ResponseEntity<Void> removeRecipeFromMealPlan(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long recipeId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        mealPlanService.removeRecipeFromMealPlan(user.getEmail(), date, recipeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{date}/notes")
    public ResponseEntity<NoteDTO> addNoteToMealPlan(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody NoteDTO noteDTO
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        NoteDTO createdNote = mealPlanService.addNoteToMealPlan(user.getEmail(), date, noteDTO);
        return ResponseEntity.ok(createdNote);
    }

    @DeleteMapping("/{date}/notes/{noteId}")
    public ResponseEntity<Void> deleteNoteFromMealPlan(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long noteId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        mealPlanService.deleteNoteFromMealPlan(user.getEmail(), date, noteId);
        return ResponseEntity.ok().build();
    }
}
