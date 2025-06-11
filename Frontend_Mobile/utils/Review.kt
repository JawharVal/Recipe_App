package com.example.recipeapp.utils



import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import android.os.Parcelable

@Parcelize
data class Review(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("rating")
    val rating: Int,

    @SerializedName("comment")
    val comment: String,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("username")
    val username: String,

    @SerializedName("recipeId")
    val recipeId: Long
) : Parcelable
