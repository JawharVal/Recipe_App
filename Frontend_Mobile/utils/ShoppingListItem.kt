package com.example.recipeapp.utils


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShoppingListItem(
    val id: Long? = null,
    val name: String,
    val category: String,
    val count: Int = 1
) : Parcelable