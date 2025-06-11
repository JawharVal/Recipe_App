package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "recipe_submissions")
public class RecipeSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate submissionDate;

    public RecipeSubmission() {}

    public RecipeSubmission(Challenge challenge, Recipe recipe, User user, LocalDate submissionDate) {
        this.challenge = challenge;
        this.recipe = recipe;
        this.user = user;
        this.submissionDate = submissionDate;
    }

    public Long getId() { return id; }
    public Challenge getChallenge() { return challenge; }
    public Recipe getRecipe() { return recipe; }
    public User getUser() { return user; }
    public LocalDate getSubmissionDate() { return submissionDate; }

    public void setChallenge(Challenge challenge) { this.challenge = challenge; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    public void setUser(User user) { this.user = user; }
    public void setSubmissionDate(LocalDate submissionDate) { this.submissionDate = submissionDate; }
}
