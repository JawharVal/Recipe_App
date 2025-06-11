// File: com/example/recipeapp/utils/ChatDataClasses.kt

package com.example.recipeapp.utils

import com.google.gson.annotations.SerializedName

// Represents a single message in the chat
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String
)

// Represents the request body for chat completions
data class ChatRequest(
    val model: String = "gpt-4-turbo", // Update as per your requirement
    val messages: List<ChatMessage>,
    val max_tokens: Int = 700,
    val temperature: Double = 0.7,
    val n: Int = 1,
    val stop: List<String>? = null
)

// Represents the choice returned by the AI
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String
)

// Represents the response body for chat completions
data class ChatResponse(
    val id: String,
    @SerializedName("object") val objectType: String, // e.g., "chat.completion"
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
