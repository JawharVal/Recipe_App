// CreateReviewDTO.kt
package com.example.recipeapp.utils

import com.google.gson.annotations.SerializedName

data class CreateReviewDTO(
    @SerializedName("rating")
    val rating: Int,

    @SerializedName("comment")
    val comment: String


)
