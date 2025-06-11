    package com.example.recipeapp.network

    import com.example.recipeapp.Recipe
    import com.example.recipeapp.utils.CreateReviewDTO
    import com.example.recipeapp.utils.GenerationLimitResponse
    import com.example.recipeapp.utils.RecipeReportDTO
    import com.example.recipeapp.utils.Review
    import com.example.recipeapp.utils.ReviewDTO
    import okhttp3.MultipartBody
    import retrofit2.Call
    import retrofit2.http.*

    interface RecipeService {
        @GET("api/recipes")
        fun getAllRecipes(): Call<List<Recipe>>

        @GET("api/recipes/{id}")
        fun getRecipeById(@Path("id") id: Long): Call<Recipe>

        @POST("api/recipes")
        fun createRecipe(@Body recipe: Recipe): Call<Recipe>
        @PUT("api/recipes/{id}")
        fun updateRecipe(@Path("id") id: Long, @Body recipe: Recipe): Call<Recipe>

        @DELETE("api/recipes/{id}")
        fun deleteRecipe(@Path("id") id: Long, @Header("Authorization") token: String): Call<Void>

        @GET("api/recipes/search")
        fun searchRecipes(@Query("title") title: String): Call<List<Recipe>>

        @Multipart
        @POST("api/recipes/{id}/image")
        fun updateRecipeImage(
            @Path("id") id: Long,
            @Part image: MultipartBody.Part
        ): Call<String>

        @GET("api/recipes/user")
        fun getUserRecipes(@Header("Authorization") token: String): Call<List<Recipe>>
        @GET("api/recipes/user")
        suspend fun getUserRecipess(@Header("Authorization") token: String): List<Recipe>
        @HTTP(method = "DELETE", path = "api/recipes/bulk", hasBody = true)
        fun bulkDeleteRecipes(
            @Header("Authorization") token: String,
            @Body ids: List<Long>
        ): Call<Void>
        @POST("api/recipes/{id}/like")
        fun likeRecipe(
            @Path("id") recipeId: Long,
            @Header("Authorization") token: String
        ): Call<Recipe>

        @GET("api/recipes/{id}/likes")
        fun getRecipeLikes(
            @Path("id") recipeId: Long
        ): Call<Int> // Returns just the number of likes
        @GET("api/generation-limit")
        fun checkGenerationLimit(): Call<GenerationLimitResponse>
        @POST("api/generation-event")
        fun recordGenerationEvent(): Call<Void>

        @POST("/api/recipe-generation")
        fun generateRecipeWithLimit(@Body ingredients: String): Call<Recipe>

        // Add a recipe to favorites
        @POST("api/auth/favorites/{recipeId}")
        fun addFavoriteRecipe(
            @Path("recipeId") recipeId: Long
        ): Call<Void>

        // Remove a recipe from favorites
        @DELETE("api/auth/favorites/{recipeId}")
        fun removeFavoriteRecipe(
            @Path("recipeId") recipeId: Long
        ): Call<Void>

        // Get all favorite recipes
        @GET("api/auth/favorites")
        fun getFavoriteRecipes(): Call<List<Recipe>>
        // Add a new review to a recipe
        // Add a new review to a recipe
        @POST("api/reviews/recipe/{recipeId}")
        fun addReview(
            @Path("recipeId") recipeId: Long,
            @Body review: CreateReviewDTO
        ): Call<ReviewDTO> // Ensure response is ReviewDTO

        // Get all reviews for a specific recipe
        @GET("api/reviews/recipe/{recipeId}")
        fun getReviewsByRecipe(
            @Path("recipeId") recipeId: Long
        ): Call<List<ReviewDTO>> // Ensure response is List<ReviewDTO>

        // Get all reviews by a specific user
        @GET("reviews/user/{userId}")
        fun getReviewsByUser(
            @Path("userId") userId: Long
        ): Call<List<Review>>
        // In your API service interface
        @POST("api/recipes/{id}/report")
        fun reportRecipe(
            @Path("id") recipeId: Long,
            @Body reason: String? = null
        ): Call<RecipeReportDTO>

        // Update an existing review
        @PUT("reviews/{reviewId}")
        fun updateReview(
            @Path("reviewId") reviewId: Long,
            @Body review: ReviewDTO
        ): Call<Review>

        // Delete a review
        @DELETE("reviews/{reviewId}")
        fun deleteReview(
            @Path("reviewId") reviewId: Long
        ): Call<Void>

    }
