package com.example.recipeapp.utils


import com.example.recipeapp.Recipe
import com.example.recipeapp.utils.ReviewDTO
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class RecipeDeserializer : JsonDeserializer<Recipe> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Recipe {
        if (json == null || !json.isJsonObject) {
            // Return a default Recipe object if JSON is null or not an object
            return Recipe()
        }

        val jsonObject = json.asJsonObject

        // Deserialize all fields except 'reviews'
        val gson = Gson()
        val recipe = gson.fromJson(jsonObject, Recipe::class.java)

        // Handle 'reviews' field
        val reviewsElement = jsonObject.get("reviews")
        val reviews: List<ReviewDTO> = if (reviewsElement != null && reviewsElement.isJsonArray) {
            gson.fromJson(reviewsElement, object : TypeToken<List<ReviewDTO>>() {}.type)
        } else {
            emptyList()
        }

        // Return the Recipe object with 'reviews' properly set
        return recipe.copy(reviews = reviews)
    }
}
