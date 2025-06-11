
package com.example.recipeapp.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// CompletionOptions data class
data class CompletionOptions(
    val stream: Boolean,
    val temperature: Double,
    @SerializedName("maxTokens") val maxTokens: String
)

// Message data class
data class Message(
    val role: String,
    val text: String
)

// YandexGPTRequest data class
data class YandexGPTRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>
)

// AssistantMessage data class
data class AssistantMessage(
    val role: String,
    val text: String
)

// Alternative data class
data class Alternative(
    val message: AssistantMessage,
    val status: String
)

// Usage data class
data class Usage(
    val inputTextTokens: String,
    val completionTokens: String,
    val totalTokens: String
)

// Result data class
data class Result(
    val alternatives: List<Alternative>,
    val usage: Usage,
    val modelVersion: String
)

// YandexGPTResponse data class
data class YandexGPTResponse(
    val result: Result
)

interface YandexGPTService {
    @POST("foundationModels/v1/completion")
    fun generateText(
        @Body request: YandexGPTRequest
    ): Call<YandexGPTResponse>
}
