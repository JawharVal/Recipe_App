package com.example.recipeapp.utils


data class FeaturedWinner(
    val id: Long? = null,
    val userEmail: String,
    val username: String? = null, // New field for username
    val totalPoints: Int
)
