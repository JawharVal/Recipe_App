
package com.example.demo.dto;

import com.example.demo.model.Recipe;

import java.util.List;

public class ChallengeDTO {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String deadline;
    private int points;

    private boolean featured;
    private boolean active;

    private List<RecipeDTO> submittedRecipes;

    private int maxSubmissions;

    public int getMaxSubmissions() { return maxSubmissions; }
    public void setMaxSubmissions(int maxSubmissions) { this.maxSubmissions = maxSubmissions; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public List<RecipeDTO> getSubmittedRecipes() {
        return submittedRecipes;
    }

    public void setSubmittedRecipes(List<RecipeDTO> submittedRecipes) {
        this.submittedRecipes = submittedRecipes;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

}
