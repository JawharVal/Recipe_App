package com.example.recipeapp.utils

import android.content.Context
import com.example.recipeapp.Recipe
import com.example.recipeapp.network.RecipeService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object ApiClient {
    const val BASE_URL = "http://192.168.0.137:8081/" // Replace with your backend's URL
    //private const val BASE_URL = "http://172.20.10.3:8081/" // Replace with your backend's URL
    //private const val BASE_URL = "http://192.168.0.145:8081/" //for same wifi ip add
    //private const val BASE_URL = "https://recipeappbackk-production.up.railway.app/" //FOR DEPLOYMENTS SERVER

    //private const val BASE_URL = "http://192.168.0.137:8081/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val recipeService: RecipeService = retrofit.create(RecipeService::class.java)
    val challengeApi: ChallengeApi = retrofit.create(ChallengeApi::class.java)
    fun getRetrofit(context: Context): Retrofit {
        val authInterceptor = AuthInterceptor(context)

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
// Create a Gson instance with the custom deserializer
        val gson = GsonBuilder()
            .registerTypeAdapter(Recipe::class.java, RecipeDeserializer())
            .create()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // Use the client with the interceptor
            .addConverterFactory(ScalarsConverterFactory.create())
            // 2) Then add Gson converter
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    }
    // Existing RecipeService
    fun getRecipeService(context: Context): RecipeService {
        return getRetrofit(context).create(RecipeService::class.java)
    }
    fun getAuthService(context: Context): AuthService {
        return getRetrofit(context).create(AuthService::class.java)
    }
    // ShoppingService
    fun getShoppingService(context: Context): ShoppingService {
        return getRetrofit(context).create(ShoppingService::class.java)
    }
    fun getMealPlanService(context: Context): MealPlanService {
        return getRetrofit(context).create(MealPlanService::class.java)
    }
    fun getBookService(context: Context): BookService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Use the client with the interceptor
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(BookService::class.java)
    }
}
