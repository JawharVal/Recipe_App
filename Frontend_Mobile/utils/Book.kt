package com.example.recipeapp.utils

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    val id: Long? = null,
    val title: String = "",
    val description: String = "",
    @SerializedName("authorId")
    val authorId: Long? = null,
    @SerializedName("recipeIds")
    var recipeIds: List<Long> = emptyList(),
    var color: String = "#866232",
    val isPublic: Boolean = false
) : Parcelable
