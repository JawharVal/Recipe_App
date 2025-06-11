package com.example.recipeapp.utils

// ReviewReportDTO.kt
data class ReviewReportDTO(
    val id: Long,
    val reviewId: Long,
    val reporterId: Long,
    val reason: String?,
    val reportedAt: String
)
