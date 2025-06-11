package com.example.recipeapp.utils


import com.google.gson.annotations.SerializedName

data class RecipeDTO(
    val id: Long?,
    val title: String,
    @SerializedName("authorUsername")
    val author: String,
    val prepTime: String,
    val cookTime: String,
    val ingredients: String,
    val instructions: String,
    val notes: String,
    val authorId: Long?,
    val imageUri: String?,
    val url: String,
    val servings: String,
    val tags: List<String>,
    val difficulty: String,
    val cuisine: String,
    val source: String,
    val video: String,
    val calories: String,
    val carbohydrates: String,
    val protein: String,
    val fat: String,
    val sugar: String,
    @SerializedName("public")
    val isPublic: Boolean
)
