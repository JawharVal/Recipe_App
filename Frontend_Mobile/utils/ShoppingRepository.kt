package com.example.recipeapp.utils


import android.content.Context
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ShoppingRepository(private val context: Context) {

    private val shoppingService: ShoppingService = ApiClient.getShoppingService(context)

    fun getAllItems(onResult: (List<ShoppingListItem>?) -> Unit) {
        shoppingService.getAllItems().enqueue(object : Callback<List<ShoppingListItem>> {
            override fun onResponse(call: Call<List<ShoppingListItem>>, response: Response<List<ShoppingListItem>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("ShoppingRepository", "Error fetching items: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<ShoppingListItem>>, t: Throwable) {
                Log.e("ShoppingRepository", "Network error: ${t.message}")
                onResult(null)
            }
        })
    }

    fun addItem(item: ShoppingListItem, onResult: (ShoppingListItem?) -> Unit) {
        shoppingService.addItem(item).enqueue(object : Callback<ShoppingListItem> {
            override fun onResponse(call: Call<ShoppingListItem>, response: Response<ShoppingListItem>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("ShoppingRepository", "Error adding item: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<ShoppingListItem>, t: Throwable) {
                Log.e("ShoppingRepository", "Network error: ${t.message}")
                onResult(null)
            }
        })
    }

    fun updateItem(id: Long, item: ShoppingListItem, onResult: (ShoppingListItem?) -> Unit) {
        shoppingService.updateItem(id, item).enqueue(object : Callback<ShoppingListItem> {
            override fun onResponse(call: Call<ShoppingListItem>, response: Response<ShoppingListItem>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("ShoppingRepository", "Error updating item: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<ShoppingListItem>, t: Throwable) {
                Log.e("ShoppingRepository", "Network error: ${t.message}")
                onResult(null)
            }
        })
    }

    fun deleteItem(id: Long, onResult: (Boolean) -> Unit) {
        shoppingService.deleteItem(id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("ShoppingRepository", "Network error: ${t.message}")
                onResult(false)
            }
        })
    }

    fun bulkDeleteItems(itemIds: List<Long>, onResult: (Boolean) -> Unit) {
        shoppingService.bulkDeleteItems(itemIds).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("ShoppingRepository", "Network error: ${t.message}")
                onResult(false)
            }
        })
    }
}