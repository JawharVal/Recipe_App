package com.example.demo.controller;

import com.example.demo.dto.RecipeDTO;
import com.example.demo.dto.StatsDTO;
import com.example.demo.dto.UserActivityDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import com.example.demo.repositories.RecipeRepository;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final UserService userService;
    @Autowired
    private RecipeService recipeService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private RecipeReportService recipeReportService;

    @Autowired
    private ReviewReportService reviewReportService;
    public AdminController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/users/{userId}/recipes")
    public ResponseEntity<List<RecipeDTO>> getRecipesByUser(@PathVariable Long userId) {

        User user = userService.getUserEntityById(userId);

        List<Recipe> userRecipes = recipeService.getRecipesByUserEmail(user.getEmail());

        List<RecipeDTO> recipeDTOs = userRecipes.stream()
                .map(recipeService::mapEntityToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(recipeDTOs);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }
    @GetMapping("/stats")
    public ResponseEntity<StatsDTO> getGlobalStats() {
        long totalUsers         = userService.countUsers();
        long totalRecipes       = recipeService.countRecipes();
        long totalLikes         = recipeService.countTotalLikes();
        long totalComments      = reviewService.countReviews();
        long totalRecipeReports = recipeReportService.countRecipeReports();
        long totalReviewReports = reviewReportService.countReviewReports();

        return ResponseEntity.ok(
                new StatsDTO(
                        totalUsers,
                        totalRecipes,
                        totalLikes,
                        totalComments,
                        totalRecipeReports,
                        totalReviewReports
                )
        );
    }
    @Autowired
    private RecipeRepository recipeRepository;

    @GetMapping("/active-users")
    public ResponseEntity<List<UserActivityDTO>> getTopActiveUsers() {
        return ResponseEntity.ok(recipeRepository.findTopFollowedUsers());
    }
}
