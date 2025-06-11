package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate date;

    @ManyToMany
    @JoinTable(
            name = "mealplan_recipes",
            joinColumns = @JoinColumn(name = "mealplan_id"),
            inverseJoinColumns = @JoinColumn(name = "recipe_id")
    )
    private List<Recipe> recipes = new ArrayList<>();

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Note> notes = new ArrayList<>();

    public MealPlan() {}

    public MealPlan(User user, LocalDate date) {
        this.user = user;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }
}
