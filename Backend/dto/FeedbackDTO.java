package com.example.demo.dto;

import java.time.LocalDateTime;

public class FeedbackDTO {
    private Long id;
    private Long userId;
    private String comment;
    private LocalDateTime createdAt;

    public FeedbackDTO() {}

    public FeedbackDTO(Long id, Long userId, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public FeedbackDTO(String comment) {
    }

    // Getters and setters for all fields...
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
