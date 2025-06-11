package com.example.demo.controller;

import com.example.demo.dto.ReviewDTO;
import com.example.demo.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/recipe/{recipeId}")
    @Operation(summary = "Add a review to a recipe", description = "Allows an authenticated user to add a comment and rating to a recipe.")
    @ApiResponse(responseCode = "201", description = "Review added successfully")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewDTO> addReview(@PathVariable Long recipeId, @RequestBody ReviewDTO reviewDTO) {
        ReviewDTO createdReview = reviewService.addReview(recipeId, reviewDTO);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    @GetMapping("/recipe/{recipeId}")
    @Operation(summary = "Get all reviews for a recipe", description = "Retrieves all comments and ratings for a specific recipe.")
    @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    public ResponseEntity<List<ReviewDTO>> getReviewsByRecipe(@PathVariable Long recipeId) {
        List<ReviewDTO> reviews = reviewService.getReviewsByRecipe(recipeId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all reviews by a user", description = "Retrieves all reviews made by a specific user.")
    @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    public ResponseEntity<List<ReviewDTO>> getReviewsByUser(@PathVariable Long userId) {
        List<ReviewDTO> reviews = reviewService.getReviewsByUser(userId);
        return ResponseEntity.ok(reviews);
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update a review", description = "Allows an authenticated user to update their own review.")
    @ApiResponse(responseCode = "200", description = "Review updated successfully")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewDTO> updateReview(@PathVariable Long reviewId, @RequestBody ReviewDTO reviewDTO) {
        ReviewDTO updatedReview = reviewService.updateReview(reviewId, reviewDTO);
        return ResponseEntity.ok(updatedReview);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete a review", description = "Allows an authenticated user to delete their own review.")
    @ApiResponse(responseCode = "204", description = "Review deleted successfully")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

}
