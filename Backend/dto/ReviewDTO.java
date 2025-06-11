package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class ReviewDTO {

    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private int rating;

    @Getter
    @Setter
    private String comment;

    @Getter
    @Setter
    private Long userId;

    @Getter
    @Setter
    private String username;

    @Getter
    @Setter
    private Long recipeId;

    public Long getId() {
        return id;
    }
    private LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    // Constructors
    public ReviewDTO() {}

    public ReviewDTO(Long id, int rating, String comment, Long userId, String username, Long recipeId) {
        this.id = id;
        this.rating = rating;
        this.comment = comment;
        this.userId = userId;
        this.username = username;
        this.recipeId = recipeId;
    }

}
