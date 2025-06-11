package com.example.recipeapp.utils


data class GlobalLeaderboardEntry(
    val id: Long,
    val userEmail: String,
    val username: String?,
    val totalPoints: Int
)
