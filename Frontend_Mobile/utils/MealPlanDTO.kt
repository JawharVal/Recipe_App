package com.example.recipeapp.utils


import com.google.gson.annotations.SerializedName

data class MealPlanDTO(
    val id: Long,
    val date: String, // ISO date string, e.g., "2024-12-19"
    val recipes: List<RecipeDTO>,
    val notes: List<NoteDTO>
)
