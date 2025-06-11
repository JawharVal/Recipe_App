package com.example.recipeapp.utils



import retrofit2.Call
import retrofit2.http.*

interface ShoppingService {

    @GET("api/shopping-list")
    fun getAllItems(): Call<List<ShoppingListItem>>

    @POST("api/shopping-list")
    fun addItem(@Body item: ShoppingListItem): Call<ShoppingListItem>

    @PUT("api/shopping-list/{id}")
    fun updateItem(@Path("id") id: Long, @Body item: ShoppingListItem): Call<ShoppingListItem>

    @DELETE("api/shopping-list/{id}")
    fun deleteItem(@Path("id") id: Long): Call<Void>

    @HTTP(method = "DELETE", path = "api/shopping-list/bulk", hasBody = true)
    fun bulkDeleteItems(@Body itemIds: List<Long>): Call<Void>
}
