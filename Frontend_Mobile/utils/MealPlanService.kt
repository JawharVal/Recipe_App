package com.example.recipeapp.utils

import retrofit2.Call
import retrofit2.http.*

interface MealPlanService {

    @GET("api/mealplans")
    fun getAllMealPlans(): Call<List<MealPlanDTO>>

    @GET("api/mealplans/{date}")
    fun getMealPlanForDate(
        @Path("date") date: String // ISO date string, e.g., "2024-12-19"
    ): Call<MealPlanDTO>

    @POST("api/mealplans/{date}/recipes/{recipeId}")
    fun addRecipeToMealPlan(
        @Path("date") date: String,
        @Path("recipeId") recipeId: Long
    ): Call<Void>

    @DELETE("api/mealplans/{date}/recipes/{recipeId}")
    fun removeRecipeFromMealPlan(
        @Path("date") date: String,
        @Path("recipeId") recipeId: Long
    ): Call<Void>

    @POST("api/mealplans/{date}/notes")
    fun addNoteToMealPlan(
        @Path("date") date: String,
        @Body noteDTO: NoteDTO
    ): Call<NoteDTO>

    @DELETE("api/mealplans/{date}/notes/{noteId}")
    fun deleteNoteFromMealPlan(
        @Path("date") date: String,
        @Path("noteId") noteId: Long
    ): Call<Void>
}
