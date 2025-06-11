package com.example.recipeapp.utils


import android.content.Context

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object IngredientsLoader {
    private var knownIngredientsSet: Set<String>? = null

    fun loadKnownIngredients(context: Context): Set<String> {
        if (knownIngredientsSet != null) {
            return knownIngredientsSet!!
        }

        val jsonString = context.assets.open("test.json").bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<RecipeIngredientsData>>() {}.type
        val recipes = Gson().fromJson<List<RecipeIngredientsData>>(jsonString, listType)

        val allIngredients = recipes.flatMap { it.ingredients }.map { it.lowercase().trim() }
        knownIngredientsSet = allIngredients.toSet()

        return knownIngredientsSet!!
    }
}
