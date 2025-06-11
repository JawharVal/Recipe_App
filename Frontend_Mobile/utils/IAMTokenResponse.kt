package com.example.recipeapp.utils

// File: com/example/recipeapp/network/IAMTokenResponse.kt

import com.google.gson.annotations.SerializedName

data class IAMTokenResponse(
    @SerializedName("iamToken") val iamToken: String,
    @SerializedName("expiresAt") val expiresAt: String
)
