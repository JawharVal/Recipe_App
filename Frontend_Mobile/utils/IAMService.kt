package com.example.recipeapp.utils
// File: com/example/recipeapp/network/IAMService.kt

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class IAMTokenRequest(
    @SerializedName("yandexPassportOauthToken") val yandexPassportOauthToken: String
)

interface IAMService {
    @POST("v1/tokens")
    fun exchangeToken(@Body request: IAMTokenRequest): Call<IAMTokenResponse>
}
