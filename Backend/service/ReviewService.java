package com.example.demo.service;

import com.example.demo.dto.ReviewDTO;
import java.util.List;

public interface ReviewService {
    ReviewDTO addReview(Long recipeId, ReviewDTO reviewDTO);
    List<ReviewDTO> getReviewsByRecipe(Long recipeId);
    List<ReviewDTO> getReviewsByUser(Long userId);
    // Optional: Update and delete methods
    ReviewDTO updateReview(Long reviewId, ReviewDTO reviewDTO);
    void deleteReview(Long reviewId);
    long countReviews();
}
