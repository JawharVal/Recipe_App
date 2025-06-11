package com.example.recipeapp.utils

data class SubscriptionRequest(
    val subscriptionType: String, // e.g., "PLUS", "PRO"
    val durationMonths: Int = 1 // Default to 1 month
)