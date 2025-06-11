package com.example.recipeapp

import android.os.Parcelable
import com.example.recipeapp.utils.ReviewDTO
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Recipe(
    @SerializedName("id")
    val id: Long? = null,
    val title: String = "",
    @SerializedName("authorUsername")
    val author: String = "",
    val prepTime: String = "",
    val cookTime: String = "",
    val ingredients: String = "",
    val instructions: String = "",
    val notes: String = "",
    val authorId: Long? = null,
    val imageUri: String? = null,
    val url: String = "",
    val servings: String = "1",
    val tags: List<String> = emptyList(),
    val difficulty: String = "Not set",
    val cuisine: String = "Not set",
    val source: String = "",
    val video: String = "",
    val calories: String = "",
    val carbohydrates: String = "",
    val protein: String = "",
    val fat: String = "",
    val sugar: String = "",
    @SerializedName("public")
    val isPublic: Boolean = true,

    @SerializedName("reviews")
    val reviews: List<ReviewDTO>? = null,
    val createdAt: String? = null,
    val likes: Int = 0,
    var likedByUser: Boolean = false,
    var isAiGenerated: Boolean = false,
) : Parcelable {
    val averageRating: Float
        get() = if (!reviews.isNullOrEmpty()) {
            reviews.map { it.rating }.average().toFloat()
        } else {
            0f
        }

}