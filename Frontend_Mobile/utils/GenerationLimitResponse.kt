package com.example.recipeapp.utils

data class GenerationLimitResponse(
    val allowed: Boolean,
    val remaining: Int,
    val currentCount: Int,
    val limit: Int
)