package com.example.recipeapp.utils

import com.google.gson.annotations.SerializedName

data class BookDTO(
    val id: Long? = null,
    val title: String,
    val description: String,
    @SerializedName("authorId")
    val authorId: Long,
    @SerializedName("recipeIds")
    val recipeIds: List<Long> = listOf(),
    var color: String = "#866232",
    var isPublic: Boolean = false

)
