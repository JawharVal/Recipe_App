package com.example.recipeapp.utils

import com.example.recipeapp.Recipe

data class ChallengeDTO(
    val id: Long,
    val title: String,
    val description: String,
    val imageUrl: String,
    val deadline: String,
    val points: Int,
    val active: Boolean,
    val submittedRecipes: List<Recipe> = emptyList(),
    val maxSubmissions: Int,
    val featured: Boolean = false // Default is false
)
