// File: com/example/recipeapp/utils/BookRepository.kt

package com.example.recipeapp.utils

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookRepository(private val context: android.content.Context) {

    private val bookService: BookService = ApiClient.getBookService(context)

    fun getBooksByUserId(userId: Long, onResult: (List<Book>?) -> Unit) {
        val token = AuthPreferences.getToken(context) ?: run {
            onResult(null)
            return
        }
        bookService.getBooksByUserId(userId, "Bearer $token").enqueue(object : Callback<List<Book>> {
            override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("BookRepository", "Error fetching books by user ID: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                Log.e("BookRepository", "Network error: ${t.message}", t)
                onResult(null)
            }
        })
    }
    fun getBooksByAuthorId(authorId: Long, onResult: (List<Book>?) -> Unit) {
        val token = AuthPreferences.getToken(context) ?: run {
            onResult(null)
            return
        }
        bookService.getBooksByAuthorId(authorId, "Bearer $token").enqueue(object : Callback<List<Book>> {
            override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("BookRepository", "Error fetching books by author ID: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                Log.e("BookRepository", "Network error: ${t.message}", t)
                onResult(null)
            }
        })
    }

    fun getBookById(bookId: Long, onResult: (Book?) -> Unit) {
        val token = AuthPreferences.getToken(context) ?: run {
            Log.e("BookRepository", "Token is null. Cannot fetch book by ID.")
            onResult(null)
            return
        }
        Log.d("BookRepository", "Fetching book with ID: $bookId")
        bookService.getBookById(bookId, "Bearer $token").enqueue(object : Callback<Book> {
            override fun onResponse(call: Call<Book>, response: Response<Book>) {
                if (response.isSuccessful) {
                    Log.d("BookRepository", "Successfully fetched book: ${response.body()}")
                    onResult(response.body())
                } else {
                    Log.e("BookRepository", "Error fetching book by ID: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<Book>, t: Throwable) {
                Log.e("BookRepository", "Network error: ${t.message}", t)
                onResult(null)
            }
        })
    }
    fun getPublicBookById(bookId: Long, onResult: (Book?) -> Unit) {
        val token = AuthPreferences.getToken(context) ?: run {
            onResult(null)
            return
        }
        bookService.getPublicBookById(bookId, "Bearer $token").enqueue(object : Callback<Book> {
            override fun onResponse(call: Call<Book>, response: Response<Book>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("BookRepository", "Error fetching public book by ID: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<Book>, t: Throwable) {
                Log.e("BookRepository", "Network error: ${t.message}", t)
                onResult(null)
            }
        })
    }

    fun getUserBooks(onResult: (List<Book>?) -> Unit) {
        val userId = AuthPreferences.getUserId(context) ?: run {
            Log.e("BookRepository", "User ID is null. Cannot fetch books.")
            onResult(null)
            return
        }
        val token = AuthPreferences.getToken(context) ?: run {
            Log.e("BookRepository", "Token is null. Cannot fetch books.")
            onResult(null)
            return
        }
        Log.d("BookRepository", "Fetching books for user ID: $userId")
        bookService.getBooksByUserId(userId, "Bearer $token").enqueue(object : Callback<List<Book>> {
            override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                if (response.isSuccessful) {
                    Log.d("BookRepository", "Successfully fetched books: ${response.body()}")
                    onResult(response.body())
                } else {
                    Log.e("BookRepository", "Error fetching books: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                Log.e("BookRepository", "Network error: ${t.message}", t)
                onResult(null)
            }
        })
    }

    fun createBook(bookDTO: BookDTO, onResult: (Book?) -> Unit) {
        val token = AuthPreferences.getToken(context) ?: run {
            onResult(null)
            return
        }

        // ✅ Log the request payload
        Log.d("BookRepository", "Creating book: $bookDTO")

        bookService.createBook(bookDTO, "Bearer $token").enqueue(object : Callback<Book> {
            override fun onResponse(call: Call<Book>, response: Response<Book>) {
                if (response.isSuccessful) {
                    Log.d("BookRepository", "Book created successfully: ${response.body()}")
                    onResult(response.body())
                } else {
                    Log.e("BookRepository", "Error creating book: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<Book>, t: Throwable) {
                Log.e("BookRepository", "Network error: ${t.message}", t)
                onResult(null)
            }
        })
    }



    fun updateBook(id: Long, bookDTO: BookDTO, onResult: (Book?) -> Unit) {
        val token = AuthPreferences.getToken(context) ?: run {
            onResult(null)
            return
        }
        bookService.updateBook(id, bookDTO, "Bearer $token").enqueue(object : Callback<Book> {
            override fun onResponse(call: Call<Book>, response: Response<Book>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("BookRepository", "Error updating book: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<Book>, t: Throwable) {
                Log.e("BookRepository", "Network error: ${t.message}", t)
                onResult(null)
            }
        })
    }

    fun deleteBook(id: Long, onResult: (Boolean) -> Unit) {
        val token = AuthPreferences.getToken(context) ?: run {
            onResult(false)
            return
        }
        bookService.deleteBook(id, "Bearer $token").enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("BookRepository", "Network error: ${t.message}", t)
                onResult(false)
            }
        })
    }


    fun addRecipeToBook(bookId: Long, recipeId: Long, onResult: (Boolean) -> Unit) {
        val token = AuthPreferences.getToken(context) ?: run {
            onResult(false)
            return
        }
        bookService.addRecipeToBook(bookId, recipeId, "Bearer $token").enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("BookRepository", "Failed to add recipe to book: ${t.message}", t)
                onResult(false)
            }
        })
    }

    fun removeRecipeFromBook(bookId: Long, recipeId: Long, onResult: (Boolean) -> Unit) {
        val token = AuthPreferences.getToken(context) ?: run {
            onResult(false)
            return
        }
        bookService.removeRecipeFromBook(bookId, recipeId, "Bearer $token").enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("BookRepository", "Failed to remove recipe from book: ${t.message}", t)
                onResult(false)
            }
        })
    }
    // In BookRepository.kt
    fun getAllPublicBooks(onResult: (List<Book>?) -> Unit) {
        // Assuming the API endpoint /api/books/public fetches all public books
        bookService.getAllPublicBooks("Bearer ${AuthPreferences.getToken(context) ?: ""}").enqueue(object : Callback<List<Book>> {
            override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("BookRepository", "Error fetching public books: ${response.code()} ${response.message()}")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                Log.e("BookRepository", "Network error: ${t.message}", t)
                onResult(null)
            }
        })
    }
    // In BookRepository.kt
    fun removeRecipeFromAllBooks(recipeId: Long, callback: (Boolean) -> Unit) {
        val userId = AuthPreferences.getUserId(context)
        // If userId is null, just signal failure immediately
        if (userId == null) {
            callback(false)
            return
        }

        // 1) Fetch all books the user owns
        getBooksByUserId(userId) { books ->
            if (books == null) {
                callback(false)
                return@getBooksByUserId
            }

            // 2) Filter the books that contain this recipe
            val booksContainingRecipe = books.filter { it.recipeIds.contains(recipeId) }
            if (booksContainingRecipe.isEmpty()) {
                // No books reference this recipe; we’re good
                callback(true)
                return@getBooksByUserId
            }

            // 3) For each book, remove recipeId from recipeIds and update that book
            var remainingToUpdate = booksContainingRecipe.size

            booksContainingRecipe.forEach { book ->
                // Remove the recipe ID from the book’s recipeIds
                val updatedRecipeIds = book.recipeIds.filter { it != recipeId }

                // Build a BookDTO that updateBook(...) expects
                val updatedBookDTO = BookDTO(
                    title = book.title,
                    description = book.description,
                    authorId = book.authorId!!,
                    recipeIds = updatedRecipeIds,
                    color = book.color
                )

                // If the book itself has no ID, skip updating it
                val bookId = book.id ?: run {
                    remainingToUpdate--
                    if (remainingToUpdate == 0) {
                        callback(true)
                    }
                    return@forEach
                }

                // 4) Call updateBook(id: Long, bookDTO: BookDTO, onResult)
                updateBook(bookId, updatedBookDTO) { updatedBook ->
                    remainingToUpdate--
                    if (updatedBook == null) {
                        // If any update fails, signal failure
                        callback(false)
                        return@updateBook
                    }

                    // If everything updated successfully, signal success
                    if (remainingToUpdate == 0) {
                        callback(true)
                    }
                }
            }
        }
    }

}
fun Book.toDTO(): BookDTO = BookDTO(
    id = this.id,
    title = this.title,
    description = this.description,
    authorId = this.authorId ?: 0, // Make sure your authorId is set appropriately
    recipeIds = this.recipeIds,
    color = this.color,
    isPublic = this.isPublic
)
