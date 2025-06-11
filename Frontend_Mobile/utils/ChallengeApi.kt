package com.example.recipeapp.utils

import com.example.recipeapp.Recipe
import com.example.recipeapp.utils.ChallengeDTO
import retrofit2.Call
import retrofit2.http.*

interface ChallengeApi {
    @GET("api/challenges")
    suspend fun getAllChallenges(): List<ChallengeDTO>
    @GET("/api/challenges/leaderboard/global")
    suspend fun getGlobalLeaderboard(): List<GlobalLeaderboardEntry>
    @GET("api/challenges/{id}/submitted")
    suspend fun getSubmittedRecipes(@Path("id") id: Long): List<Recipe>
    @GET("api/challenges/{id}")
    suspend fun getChallengeById(@Path("id") id: Long): ChallengeDTO
    @POST("api/moderate-image") // ✅ Matches backend!
    fun moderateImage(
        @Query("fileUrl") fileUrl: String
    ): Call<Map<String, Boolean>>

    @GET("api/challenges/featured/names")
    fun getFeaturedChallengeNames(): Call<List<String>>

    @GET("api/recipes/user")
    suspend fun getUserRecipes(@Header("Authorization") token: String): List<Recipe>
    @GET("api/challenges/featuredWinners")
    suspend fun getFeaturedWinners(): List<FeaturedWinner>

    @POST("api/challenges")
    suspend fun createChallenge(@Body challengeDTO: ChallengeDTO): ChallengeDTO
    @POST("api/recipes/{id}/like")
    suspend fun likeRecipe(@Path("id") id: Long)

    @POST("api/recipes/{id}/dislike")
    suspend fun dislikeRecipe(@Path("id") id: Long)
    @POST("api/challenges/{challengeId}/submit")
    suspend fun submitRecipe(
        @Path("challengeId") challengeId: Long,
        @Query("recipeId") recipeId: Long
    ): ChallengeDTO

    @PUT("api/challenges/{id}")
    suspend fun updateChallenge(@Path("id") id: Long, @Body challengeDTO: ChallengeDTO): ChallengeDTO

    @DELETE("api/challenges/{id}")
    suspend fun deleteChallenge(@Path("id") id: Long)

    @POST("api/challenges/{id}/vote")
    suspend fun voteChallenge(@Path("id") id: Long, @Query("voteValue") voteValue: Int)

    @GET("api/challenges/leaderboard")
    suspend fun getLeaderboard(): List<ChallengeDTO>
}
