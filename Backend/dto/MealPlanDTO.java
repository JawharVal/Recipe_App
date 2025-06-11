package com.example.demo.dto;

import java.time.LocalDate;
import java.util.List;

public class MealPlanDTO {
    private Long id;
    private LocalDate date;
    private List<RecipeDTO> recipes;
    private List<NoteDTO> notes;

    public MealPlanDTO() {}

    public MealPlanDTO(Long id, LocalDate date, List<RecipeDTO> recipes, List<NoteDTO> notes) {
        this.id = id;
        this.date = date;
        this.recipes = recipes;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<RecipeDTO> getRecipes() {
        return recipes;
    }

    public List<NoteDTO> getNotes() {
        return notes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setRecipes(List<RecipeDTO> recipes) {
        this.recipes = recipes;
    }

    public void setNotes(List<NoteDTO> notes) {
        this.notes = notes;
    }
}
