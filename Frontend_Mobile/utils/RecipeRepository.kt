package com.example.recipeapp.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.auth0.android.jwt.BuildConfig
import com.example.recipeapp.Recipe
import com.example.recipeapp.network.CompletionOptions
import com.example.recipeapp.network.Message
import com.example.recipeapp.network.RecipeService
import com.example.recipeapp.network.YandexGPTClient
import com.example.recipeapp.network.YandexGPTRequest
import com.example.recipeapp.network.YandexGPTResponse
import com.example.recipeapp.network.YandexGPTService
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecipeRepository(private val context: Context) {

    private val recipeService: RecipeService = ApiClient.getRecipeService(context)
    private val yandexGPTService: YandexGPTService = YandexGPTClient.instance.create(YandexGPTService::class.java)
    private val authService: AuthService = ApiClient.getAuthService(context)
    private fun preprocessAssistantMessage(original: String): String {
        var text = original.trim()

        // Remove leading triple backticks
        if (text.startsWith("```")) {
            text = text.drop(3).trim()
        }

        // Remove trailing triple backticks
        if (text.endsWith("```")) {
            text = text.dropLast(3).trim()
        }

        // Extract only the JSON part between '{' and '}'
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            text = text.substring(startIndex, endIndex + 1).trim()
        }

        return text
    }

    suspend fun getUserByIdSuspend(userId: Long): UserDTO? {
        return try {
            authService.getUserByIdSuspend(userId)
        } catch (e: Exception) {
            Log.e("RecipeRepository", "Error fetching user (suspend): ${e.message}")
            null
        }
    }

    fun generateRecipeFromAI(
        ingredients: String,
        onResult: (Recipe?) -> Unit
    ) {
        // Simple validation
        if (ingredients.isBlank() || ingredients.split(",").any { it.trim().isEmpty() }) {
            onResult(null) // Invalid ingredients
            return
        }

        // Construct the prompt
        val prompt = """
            Create a detailed recipe using the following ingredients: $ingredients.
            The recipe should include the following sections:
            - Title
            - Ingredients
            - Instructions
            - Servings
            - Prep Time
            - Cook Time
            - Notes
            - Difficulty
            - Cuisine
            - Source
            - Video URL
            - Calories
            - Carbohydrates
            - Protein
            - Fat
            - Sugar
            - Tags (comma-separated)
            - Public (true/false)
        
            Please format the recipe as JSON matching the following structure:
        
            {
                "id": null,
                "title": "String",
                "authorUsername": "String",
                "prepTime": "String",
                "cookTime": "String",
                "ingredients": "String",
                "instructions": "String",
                "notes": "String",
                "authorId": null,
                "imageUri": "String",
                "url": "String",
                "servings": "String",
                "tags": ["String"],
                "difficulty": "String",
                "cuisine": "String",
                "source": "String",
                "video": "String",
                "calories": "String",
                "carbohydrates": "String",
                "protein": "String",
                "fat": "String",
                "sugar": "String",
                "isPublic": true
            }
        """.trimIndent()

        val modelUri = "gpt://b1gt9p4bqk5812pe5spj/yandexgpt-lite/latest"

        // Define the completion options
        val completionOptions = CompletionOptions(
            stream = false,
            temperature = 0.6,
            maxTokens = "2000" // As per your Postman request
        )

        // Define the messages
        val messages = listOf(
            Message(
                role = "system",
                text = "You are a creative assistant that creates unique and detailed JSON recipes. Each recipe must have a distinct title."
            ),
            Message(
                role = "user",
                text = """
                Create a detailed and unique recipe ( you should not repeat the same recipe if you already gave it) using the following ingredients: $ingredients.
                VERY IMPORTANT: In the 'Ingredients' field, separate each ingredient using a newline character (\n) so that each ingredient appears on a separate line.
                VERY IMPORTANT: In the instructions field, please ensure each step is separated by a newline character (\n). 
                 Example:
                "1. Do this\n2. Then do that\n3. Finally do this."
                Dont forget to give the quantity of each ingredient as well.
                For Cuisine it means you need to write the origin country of the recipe, for example "Mexican".
                IMPORTANT: The "isPublic" field **must** always be `false` in the JSON response.
                VERY IMPORTANT: THE ANSWERS MUST BE IN ENLGISH LANGUAGE
                      
                The recipe should include the following sections:
                - Title (must be unique)
                - Ingredients
                - Instructions
                - Servings
                - Prep Time ( write the number only without unit ) 
                - Cook Time ( write the number only without unit ) 
                - Notes
                - Difficulty
                - Cuisine
                - Source (should be an address)
                - Video URL (should be an address)
                - Calories ( number only ) 
                - Carbohydrates ( number only ) 
                - Protein ( number only ) 
                - Fat ( number only ) 
                - Sugar ( number only ) 
                - Tags (comma-separated)
                - Public (false)

                IMPORTANT: Return the recipe only as a JSON object and do not include any extra text, explanations, or formatting outside the JSON.

                The JSON structure must match exactly:
                {
                    "id": null,
                    "title": "String",
                    "authorUsername": "String",
                    "prepTime": "String",
                    "cookTime": "String",
                    "ingredients": "String",
                    "instructions": "String",
                    "notes": "String",
                    "authorId": null,
                    "imageUri": "String",
                    "url": "String",
                    "servings": "String",
                    "tags": ["String"],
                    "difficulty": "String",
                    "cuisine": "String",
                    "source": "String",
                    "video": "String",
                    "calories": "String",
                    "carbohydrates": "String",
                    "protein": "String",
                    "fat": "String",
                    "sugar": "String",
                    "isPublic": false
                }
            """.trimIndent()
            )
        )



        val yandexGPTRequest = YandexGPTRequest(
            modelUri = modelUri,
            completionOptions = completionOptions,
            messages = messages
        )

        // Make the API call
        yandexGPTService.generateText(yandexGPTRequest).enqueue(object : Callback<YandexGPTResponse> {
            override fun onResponse(call: Call<YandexGPTResponse>, response: Response<YandexGPTResponse>) {
                if (response.isSuccessful) {
                    val aiResponse = response.body()?.result
                    if (aiResponse != null && aiResponse.alternatives.isNotEmpty()) {
                        val assistantMessage = aiResponse.alternatives[0].message.text

// Preprocess assistantMessage to remove code fences and extra formatting
                        val cleanedMessage = preprocessAssistantMessage(assistantMessage)

                        try {
                            val gson = Gson()
                            val recipe = gson.fromJson(cleanedMessage, Recipe::class.java)
                            if (recipe != null) {
                                recipe.isAiGenerated = true
                            }
                            onResult(recipe)
                        } catch (e: Exception) {
                            Log.e("RecipeRepository", "Error parsing AI response: ${e.message}")
                            onResult(null)
                        }
                    } else {
                        Log.e("RecipeRepository", "AI response is empty or no alternatives.")
                        onResult(null)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("RecipeRepository", "AI API Error: ${response.code()} ${response.message()} $errorBody")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<YandexGPTResponse>, t: Throwable) {
                Log.e("RecipeRepository", "AI API Network Error: ${t.message}")
                onResult(null)
            }
        })
    }


    fun getAllRecipes(onResult: (List<Recipe>?) -> Unit) {
        recipeService.getAllRecipes().enqueue(object : Callback<List<Recipe>> {
            override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                onResult(response.body())
            }

            override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                onResult(null)
            }
        })
    }
    // Add this method for fetching user-specific recipes
    fun getUserRecipes(token: String, onResult: (List<Recipe>?) -> Unit) {
        recipeService.getUserRecipes("Bearer $token").enqueue(object : Callback<List<Recipe>> {
            override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                if (response.isSuccessful) {
                    onResult(response.body()) // Pass the recipes to the callback
                } else {
                    println("Error fetching user recipes: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                println("Network error: ${t.message}")
                onResult(null) // Return null on failure
            }
        })
    }
    fun deleteSelectedRecipes(token: String, recipeIds: List<Long>, onResult: (Boolean) -> Unit) {
        recipeService.bulkDeleteRecipes("Bearer $token", recipeIds).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.e("RecipeRepository", "Error deleting recipes: ${response.code()} ${response.message()}")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("RecipeRepository", "Network error: ${t.message}", t)
                onResult(false)
            }
        })
    }



    fun getRecipeById(recipeId: Long, onResult: (Recipe?) -> Unit) {
        Log.d("RecipeRepository", "Fetching recipe with ID: $recipeId")
        recipeService.getRecipeById(recipeId).enqueue(object : Callback<Recipe> {
            override fun onResponse(call: Call<Recipe>, response: Response<Recipe>) {
                if (response.isSuccessful) {
                    val fetchedRecipe = response.body()
                    Log.d("RecipeRepository", "Recipe fetched successfully: $fetchedRecipe")
                    Log.d("RecipeRepository", "Fetched Reviews: ${fetchedRecipe?.reviews}")
                    onResult(fetchedRecipe)
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("RecipeRepository", "Error fetching recipe: ${response.code()} ${response.message()} $errorMsg")
                    Toast.makeText(context, "Failed to load recipe.", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<Recipe>, t: Throwable) {
                Log.e("RecipeRepository", "Network error: ${t.message}", t)
                Toast.makeText(context, "Network error while loading recipe.", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        })
    }


    fun createRecipe(recipe: Recipe, onResult: (Recipe?) -> Unit) {
        Log.d("RecipeRepository", "Creating recipe: $recipe")
        recipeService.createRecipe(recipe).enqueue(object : Callback<Recipe> {
            override fun onResponse(call: Call<Recipe>, response: Response<Recipe>) {
                if (response.isSuccessful) {
                    val createdRecipe = response.body()
                    Log.d("RecipeRepository", "Recipe created successfully: $createdRecipe")
                    onResult(createdRecipe)
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("RecipeRepository", "Error creating recipe: ${response.code()} ${response.message()} $errorMsg")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<Recipe>, t: Throwable) {
                Log.e("RecipeRepository", "Network error: ${t.message}", t)
                onResult(null)
            }
        })
    }


    fun updateRecipe(id: Long, recipe: Recipe, onResult: (Recipe?) -> Unit) {
        recipeService.updateRecipe(id, recipe).enqueue(object : Callback<Recipe> {
            override fun onResponse(call: Call<Recipe>, response: Response<Recipe>) {
                onResult(response.body())
            }

            override fun onFailure(call: Call<Recipe>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun deleteRecipe(id: Long, token: String, onResult: (Boolean) -> Unit) {
        recipeService.deleteRecipe(id, "Bearer $token").enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onResult(true) // Deletion successful
                } else {
                    println("Error deleting recipe: ${response.code()} ${response.message()}")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                println("Network error: ${t.message}")
                onResult(false)
            }
        })
    }
    fun addFavoriteRecipe(recipeId: Long, onResult: (Boolean) -> Unit) {
        recipeService.addFavoriteRecipe(recipeId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.e("RecipeRepository", "Failed to add favorite: ${response.code()} ${response.message()}")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("RecipeRepository", "Network error while adding favorite: ${t.message}", t)
                onResult(false)
            }
        })
    }

    /**
     * Removes a recipe from the user's favorites.
     */
    fun removeFavoriteRecipe(recipeId: Long, onResult: (Boolean) -> Unit) {
        recipeService.removeFavoriteRecipe(recipeId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.e("RecipeRepository", "Failed to remove favorite: ${response.code()} ${response.message()}")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("RecipeRepository", "Network error while removing favorite: ${t.message}", t)
                onResult(false)
            }
        })
    }

    /**
     * Retrieves all favorite recipes of the current user.
     */
    fun getFavoriteRecipes(onResult: (List<Recipe>?) -> Unit) {
        recipeService.getFavoriteRecipes().enqueue(object : Callback<List<Recipe>> {
            override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("RecipeRepository", "Failed to fetch favorites: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                Log.e("RecipeRepository", "Network error while fetching favorites: ${t.message}", t)
                onResult(null)
            }
        })
    }
    fun bulkDeleteUserRecipes(token: String, callback: (Boolean) -> Unit) {
        // 1) Fetch user recipes
        recipeService.getUserRecipes("Bearer $token").enqueue(object : Callback<List<Recipe>> {
            override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                val recipes = response.body()
                if (recipes != null && recipes.isNotEmpty()) {
                    val recipeIds = recipes.mapNotNull { it.id }

                    Log.d("Debug", "deleteSelectedRecipes() called with token = Bearer $token")
                    Log.d("Debug", "recipeIds = $recipeIds")

                    // 2) Bulk delete with "Bearer <token>"
                    recipeService.bulkDeleteRecipes("Bearer $token", recipeIds)
                        .enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                callback(response.isSuccessful)
                            }
                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                callback(false)
                            }
                        })
                } else {
                    // No recipes to delete; you might consider this a success
                    callback(true)
                }
            }
            override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                callback(false)
            }
        })
    }


    /**
     * Checks if a specific recipe is in the user's favorites.
     */
    fun isRecipeFavorited(recipeId: Long, onResult: (Boolean) -> Unit) {
        getFavoriteRecipes { favorites ->
            if (favorites != null) {
                onResult(favorites.any { it.id == recipeId })
            } else {
                onResult(false)
            }
        }
    }

    // New method to fetch user by ID
    fun getUserById(userId: Long, onResult: (UserDTO?) -> Unit) {
        authService.getUserById(userId).enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("RecipeRepository", "Failed to fetch user: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("RecipeRepository", "Network error while fetching user: ${t.message}", t)
                onResult(null)
            }
        })
    }
    private val sharedPreferences = context.getSharedPreferences("recipe_prefs", Context.MODE_PRIVATE)

    fun loadRemainingGenerations(): Int? {
        return sharedPreferences.getInt("remaining_generations", -1).takeIf { it != -1 }
    }

    fun saveRemainingGenerations(value: Int?) {
        sharedPreferences.edit().putInt("remaining_generations", value ?: -1).apply()
    }
    fun addReview(recipeId: Long, review: CreateReviewDTO, onResult: (ReviewDTO?) -> Unit) {
        recipeService.addReview(recipeId, review).enqueue(object : Callback<ReviewDTO> {
            override fun onResponse(call: Call<ReviewDTO>, response: Response<ReviewDTO>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("RecipeRepository", "Failed to add review: ${response.code()} ${response.message()} $errorMsg")
                    Toast.makeText(context, "Failed to add review.", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<ReviewDTO>, t: Throwable) {
                Log.e("RecipeRepository", "Network error: ${t.message}", t)
                Toast.makeText(context, "Network error while adding review.", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        })
    }
    fun getRecipesByAuthorId(authorId: Long, onResult: (List<Recipe>?) -> Unit) {
        // First, fetch all recipes (using your existing getAllRecipes method)
        getAllRecipes { recipes ->
            // Filter the list based on the authorId field
            val filteredRecipes = recipes?.filter { it.authorId == authorId }
            onResult(filteredRecipes)
        }
    }


    // Fetch reviews for a recipe
    fun getReviewsByRecipe(recipeId: Long, onResult: (List<ReviewDTO>?) -> Unit) {
        recipeService.getReviewsByRecipe(recipeId).enqueue(object : Callback<List<ReviewDTO>> {
            override fun onResponse(call: Call<List<ReviewDTO>>, response: Response<List<ReviewDTO>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("RecipeRepository", "Failed to fetch reviews: ${response.code()} ${response.message()} $errorMsg")
                    Toast.makeText(context, "Failed to fetch reviews.", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<ReviewDTO>>, t: Throwable) {
                Log.e("RecipeRepository", "Network error: ${t.message}", t)
                Toast.makeText(context, "Network error while fetching reviews.", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        })
    }


    // Update a review
    fun updateReview(reviewId: Long, review: ReviewDTO, onResult: (Review?) -> Unit) {
        recipeService.updateReview(reviewId, review).enqueue(object : Callback<Review> {
            override fun onResponse(call: Call<Review>, response: Response<Review>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("RecipeRepository", "Failed to update review: ${response.code()} ${response.message()} $errorMsg")
                    Toast.makeText(context, "Failed to update review.", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<Review>, t: Throwable) {
                Log.e("RecipeRepository", "Network error: ${t.message}", t)
                Toast.makeText(context, "Network error while updating review.", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        })
    }

    // Delete a review
    fun deleteReview(reviewId: Long, onResult: (Boolean) -> Unit) {
        recipeService.deleteReview(reviewId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.e("RecipeRepository", "Failed to delete review: ${response.code()} ${response.message()}")
                    Toast.makeText(context, "Failed to delete review.", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("RecipeRepository", "Network error: ${t.message}", t)
                Toast.makeText(context, "Network error while deleting review.", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        })
    }


    fun checkGenerationLimit(onResult: (allowed: Boolean, error: String?) -> Unit) {
        recipeService.checkGenerationLimit().enqueue(object : Callback<GenerationLimitResponse> {
            override fun onResponse(
                call: Call<GenerationLimitResponse>,
                response: Response<GenerationLimitResponse>
            ) {
                if (response.isSuccessful) {
                    val allowed = response.body()?.allowed ?: false
                    onResult(allowed, null)
                } else {
                    onResult(false, "Unable to check generation limit: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<GenerationLimitResponse>, t: Throwable) {
                onResult(false, "Network error: ${t.localizedMessage}")
            }
        })
    }


    fun recordGenerationEvent(onResult: (success: Boolean, error: String?) -> Unit) {
        // Assume a new endpoint:
        // @POST("/api/generation-event")
        // fun recordGenerationEvent(): Call<Void>
        recipeService.recordGenerationEvent().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful, if (!response.isSuccessful) "Error recording event" else null)
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                onResult(false, "Network error: ${t.localizedMessage}")
            }
        })
    }

}
