// File: Challenge.java
package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "challenges")
public class Challenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String imageUrl;
    private LocalDate deadline;
    private int points;

    private boolean active;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "challenge_id")
    private List<Recipe> submittedRecipes;

    private boolean featured = false;

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public List<RecipeSubmission> getRecipeSubmissions() {
        return recipeSubmissions;
    }

    public void setRecipeSubmissions(List<RecipeSubmission> recipeSubmissions) {
        this.recipeSubmissions = recipeSubmissions;
    }

    public List<RecipeSubmission> getSubmissions() {
        return submissions;
    }
    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)

    private List<RecipeSubmission> submissions = new ArrayList<>();


    public void setSubmissions(List<RecipeSubmission> submissions) {
        this.submissions = submissions;
    }

    public Challenge() {
        this.active = true;
    }

    public Challenge(String title, String description, String imageUrl, LocalDate deadline, int points) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.deadline = deadline;
        this.points = points;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<RecipeSubmission> recipeSubmissions = new ArrayList<>();


    public List<Recipe> getSubmittedRecipes() {
        return recipeSubmissions.stream()
                .map(RecipeSubmission::getRecipe)
                .collect(Collectors.toList());
    }

    public void setSubmittedRecipes(List<Recipe> submittedRecipes) { this.submittedRecipes = submittedRecipes; }

    private int maxSubmissions; // 🔥 NEW FIELD

    public int getMaxSubmissions() { return maxSubmissions; }
    public void setMaxSubmissions(int maxSubmissions) { this.maxSubmissions = maxSubmissions; }


}
