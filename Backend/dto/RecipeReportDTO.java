package com.example.demo.dto;

import java.time.LocalDateTime;

public class RecipeReportDTO {
    private Long id;
    private Long recipeId;
    private String recipeTitle;
    private String reporterUsername;
    private String reason;
    private LocalDateTime reportedAt;

    public RecipeReportDTO() {}

    public RecipeReportDTO(Long id, Long recipeId, String recipeTitle, String reporterUsername, String reason, LocalDateTime reportedAt) {
        this.id = id;
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.reporterUsername = reporterUsername;
        this.reason = reason;
        this.reportedAt = reportedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public String getRecipeTitle() {
        return recipeTitle;
    }

    public String getReporterUsername() {
        return reporterUsername;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public void setRecipeTitle(String recipeTitle) {
        this.recipeTitle = recipeTitle;
    }

    public void setReporterUsername(String reporterUsername) {
        this.reporterUsername = reporterUsername;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
