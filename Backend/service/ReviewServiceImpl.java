package com.example.demo.service;

import com.example.demo.dto.ReviewDTO;
import com.example.demo.model.Recipe;
import com.example.demo.model.Review;
import com.example.demo.model.User;
import com.example.demo.repositories.RecipeRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ReviewDTO addReview(Long recipeId, ReviewDTO reviewDTO) {
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Fetch user and recipe
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Optional: Prevent multiple reviews by the same user on the same recipe
        /*
        Review existingReview = reviewRepository.findByUserAndRecipe(user, recipe);
        if (existingReview != null) {
            throw new RuntimeException("You have already reviewed this recipe.");
        }
        */

        // Create and save review
        Review review = new Review();
        review.setRating(reviewDTO.getRating());
        review.setComment(reviewDTO.getComment());
        review.setUser(user);
        review.setRecipe(recipe);
        review.setCreatedAt(LocalDateTime.now());
        Review savedReview = reviewRepository.save(review);

        // Map to DTO
        return mapEntityToDTO(savedReview);
    }

    @Override
    public List<ReviewDTO> getReviewsByRecipe(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        List<Review> reviews = reviewRepository.findByRecipeIdOrderByCreatedAtDesc(recipeId);

        return reviews.stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());
    }
    @Override
    public long countReviews() {
        return reviewRepository.count();
    }
    @Override
    public List<ReviewDTO> getReviewsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Review> reviews = reviewRepository.findByUser(user);

        return reviews.stream()
                .map(this::mapEntityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewDTO updateReview(Long reviewId, ReviewDTO reviewDTO) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Check if the authenticated user is the owner of the review
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        if (!review.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not authorized to update this review.");
        }

        // Update fields
        review.setRating(reviewDTO.getRating());
        review.setComment(reviewDTO.getComment());

        Review updatedReview = reviewRepository.save(review);

        return mapEntityToDTO(updatedReview);
    }

    @Override
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Check if the authenticated user is the owner of the review
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        if (!review.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not authorized to delete this review.");
        }

        reviewRepository.delete(review);
    }

    // Utility method to map Review entity to ReviewDTO
    private ReviewDTO mapEntityToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setUserId(review.getUser().getId());
        dto.setUsername(review.getUser().getUsername());
        dto.setRecipeId(review.getRecipe().getId());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
