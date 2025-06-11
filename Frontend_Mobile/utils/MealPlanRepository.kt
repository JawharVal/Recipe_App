package com.example.recipeapp.utils

import android.content.Context
import android.util.Log

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MealPlanRepository(private val context: Context) {

    private val mealPlanService: MealPlanService = ApiClient.getMealPlanService(context)

    fun getAllMealPlans(onResult: (List<MealPlanDTO>?) -> Unit) {
        mealPlanService.getAllMealPlans().enqueue(object : Callback<List<MealPlanDTO>> {
            override fun onResponse(call: Call<List<MealPlanDTO>>, response: Response<List<MealPlanDTO>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("MealPlanRepository", "Error fetching meal plans: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<MealPlanDTO>>, t: Throwable) {
                Log.e("MealPlanRepository", "Network error: ${t.message}")
                onResult(null)
            }
        })
    }

    fun getMealPlanForDate(date: String, onResult: (MealPlanDTO?) -> Unit) {
        mealPlanService.getMealPlanForDate(date).enqueue(object : Callback<MealPlanDTO> {
            override fun onResponse(call: Call<MealPlanDTO>, response: Response<MealPlanDTO>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("MealPlanRepository", "Error fetching meal plan: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<MealPlanDTO>, t: Throwable) {
                Log.e("MealPlanRepository", "Network error: ${t.message}")
                onResult(null)
            }
        })
    }

    fun addRecipeToMealPlan(date: String, recipeId: Long, onResult: (Boolean) -> Unit) {
        mealPlanService.addRecipeToMealPlan(date, recipeId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.e("MealPlanRepository", "Error adding recipe: ${response.code()} ${response.message()}")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("MealPlanRepository", "Network error: ${t.message}")
                onResult(false)
            }
        })
    }


    fun removeRecipeFromMealPlan(date: String, recipeId: Long, onResult: (Boolean) -> Unit) {
        mealPlanService.removeRecipeFromMealPlan(date, recipeId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("MealPlanRepository", "Network error: ${t.message}")
                onResult(false)
            }
        })
    }

    fun addNoteToMealPlan(date: String, note: NoteDTO, onResult: (NoteDTO?) -> Unit) {
        mealPlanService.addNoteToMealPlan(date, note).enqueue(object : Callback<NoteDTO> {
            override fun onResponse(call: Call<NoteDTO>, response: Response<NoteDTO>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("MealPlanRepository", "Error adding note: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<NoteDTO>, t: Throwable) {
                Log.e("MealPlanRepository", "Network error: ${t.message}")
                onResult(null)
            }
        })
    }

    fun deleteNoteFromMealPlan(date: String, noteId: Long, onResult: (Boolean) -> Unit) {
        mealPlanService.deleteNoteFromMealPlan(date, noteId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.e("MealPlanRepository", "Error deleting note: ${response.code()} ${response.message()}")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("MealPlanRepository", "Network error: ${t.message}")
                onResult(false)
            }
        })
    }
}
