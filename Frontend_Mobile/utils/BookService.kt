// File: com/example/recipeapp/utils/BookService.kt

package com.example.recipeapp.utils

import retrofit2.Call
import retrofit2.http.*

interface BookService {

    @GET("/api/books/user/{userId}")
    fun getBooksByUserId(
        @Path("userId") userId: Long,
        @Header("Authorization") token: String
    ): Call<List<Book>>

    @GET("/api/books/{id}")
    fun getBookById(
        @Path("id") id: Long,
        @Header("Authorization") token: String
    ): Call<Book>

    // Existing methods...
    @GET("/api/books")
    fun getAllBooks(): Call<List<Book>>

    @POST("/api/books")
    fun createBook(
        @Body bookDTO: BookDTO,
        @Header("Authorization") token: String
    ): Call<Book>
    // In BookService.kt
    @GET("/api/books/public/{id}")
    fun getPublicBookById(
        @Path("id") id: Long,
        @Header("Authorization") token: String
    ): Call<Book>
    
    @GET("/api/books/public")
    fun getAllPublicBooks(
        @Header("Authorization") token: String
    ): Call<List<Book>>
    @PUT("/api/books/{id}")
    fun updateBook(
        @Path("id") id: Long,
        @Body bookDTO: BookDTO,
        @Header("Authorization") token: String
    ): Call<Book>
    @GET("/api/books/author/{userId}")
    fun getBooksByAuthorId(
        @Path("userId") userId: Long,
        @Header("Authorization") token: String
    ): Call<List<Book>>
    @DELETE("/api/books/{id}")
    fun deleteBook(
        @Path("id") id: Long,
        @Header("Authorization") token: String
    ): Call<Void>

    @POST("/api/books/{bookId}/recipes/{recipeId}")
    fun addRecipeToBook(
        @Path("bookId") bookId: Long,
        @Path("recipeId") recipeId: Long,
        @Header("Authorization") authHeader: String
    ): Call<Void>

    @DELETE("/api/books/{bookId}/recipes/{recipeId}")
    fun removeRecipeFromBook(
        @Path("bookId") bookId: Long,
        @Path("recipeId") recipeId: Long,
        @Header("Authorization") token: String
    ): Call<Void>
}
