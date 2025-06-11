package com.example.recipeapp.utils

data class RecipeReportDTO(
    val id: Long,
    val recipeId: Long,
    val recipeTitle: String,
    val reporterUsername: String,
    val reason: String?,
    val reportedAt: String
)
