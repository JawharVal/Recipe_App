package com.example.demo.model;

import javax.persistence.*;

@Entity
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mealplan_id")
    private MealPlan mealPlan;

    public Note() {}

    public Note(String content, MealPlan mealPlan) {
        this.content = content;
        this.mealPlan = mealPlan;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public MealPlan getMealPlan() {
        return mealPlan;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setMealPlan(MealPlan mealPlan) {
        this.mealPlan = mealPlan;
    }
}
