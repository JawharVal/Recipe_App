// ReviewDTO.kt
package com.example.recipeapp.utils

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReviewDTO(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("rating")
    val rating: Int,

    @SerializedName("comment")
    val comment: String,

    @SerializedName("userId")
    val userId: Long? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("recipeId")
    val recipeId: Long? = null,

    // This must match what the backend sends
    @SerializedName("createdAt")
    val createdAt: String? = null
) : Parcelable
