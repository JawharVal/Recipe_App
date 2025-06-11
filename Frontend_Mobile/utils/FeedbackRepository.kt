// app/src/main/java/com/example/recipeapp/utils/FeedbackRepository.kt
package com.example.recipeapp.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedbackRepository(private val context: Context) {
    private val apiService = ApiClient.getRetrofit(context).create(AuthService::class.java)

    fun submitFeedback(comment: String, onResult: (Boolean) -> Unit) {
        val feedback = FeedbackDTO(comment)

        apiService.submitFeedback(feedback).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.e("FeedbackRepository", "Failed to submit feedback: ${response.code()}")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("FeedbackRepository", "Error submitting feedback", t)
                onResult(false)
            }
        })
    }
}
