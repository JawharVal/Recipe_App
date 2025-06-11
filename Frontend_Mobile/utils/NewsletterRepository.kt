// app/src/main/java/com/example/recipeapp/utils/NewsletterRepository.kt
package com.example.recipeapp.utils

import android.content.Context
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

sealed class SubscriptionResult {
    object Success : SubscriptionResult()
    object AlreadySubscribed : SubscriptionResult()
    object NotSubscribed : SubscriptionResult()
    data class Failure(val errorCode: Int) : SubscriptionResult()
}

class NewsletterRepository(private val context: Context) {
    private val apiService = ApiClient.getRetrofit(context).create(AuthService::class.java)

    /**
     * Subscribe the user to the newsletter.
     */
    fun subscribe(onResult: (SubscriptionResult) -> Unit) {
        apiService.subscribeNewsletter().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                when (response.code()) {
                    201 -> onResult(SubscriptionResult.Success)
                    409 -> onResult(SubscriptionResult.AlreadySubscribed)
                    else -> onResult(SubscriptionResult.Failure(response.code()))
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("NewsletterRepository", "Error subscribing to newsletter", t)
                onResult(SubscriptionResult.Failure(-1))
            }
        })
    }

    /**
     * Check if the user is already subscribed to the newsletter.
     */
    fun isSubscribed(onResult: (Boolean) -> Unit) {
        apiService.isSubscribed().enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        onResult(it)
                    } ?: run {
                        onResult(false)
                    }
                } else {
                    Log.e("NewsletterRepository", "Failed to check subscription: ${response.code()}")
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                Log.e("NewsletterRepository", "Error checking subscription", t)
                onResult(false)
            }
        })
    }

    /**
     * Unsubscribe the user from the newsletter.
     */
    fun unsubscribe(onResult: (SubscriptionResult) -> Unit) {
        apiService.unsubscribeNewsletter().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                when (response.code()) {
                    200 -> onResult(SubscriptionResult.Success)
                    400 -> onResult(SubscriptionResult.NotSubscribed)
                    else -> onResult(SubscriptionResult.Failure(response.code()))
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("NewsletterRepository", "Error unsubscribing from newsletter", t)
                onResult(SubscriptionResult.Failure(-1))
            }
        })
    }
}