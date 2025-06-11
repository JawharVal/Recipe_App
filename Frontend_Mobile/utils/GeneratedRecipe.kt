package com.example.recipeapp.utils

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.recipeapp.Recipe

data class GeneratedRecipe(
    val recipe: Recipe,
    val isExpanded: MutableState<Boolean> = mutableStateOf(false)
)