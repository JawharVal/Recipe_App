package com.example.recipeapp.utils

import android.content.Context
import android.util.Base64
import com.auth0.android.jwt.JWT
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

object AuthPreferences {

    private const val PREFS_NAME = "auth_prefs"
    private const val TOKEN_KEY = "JWT_TOKEN"

    private const val KEY_USERNAME = "username"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_TOKEN = "token"

    private const val KEY_USER_ROLE = "user_role"
    fun saveToken(context: Context, token: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply()
    }
    fun isTokenValid(context: Context): Boolean {
        val token = getToken(context) ?: return false
        return try {
            val decodedJWT = JWT(token)
            !decodedJWT.isExpired(10) // Allow 10 seconds skew
        } catch (e: Exception) {
            false
        }
    }
    fun getUserRole(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_USER_ROLE, "user") // Default to "user"
    }

    fun saveUserRole(context: Context, role: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(KEY_USER_ROLE, role).apply()
    }
    fun getCurrentUserEmail(context: Context): String? {
        // Example: if you have stored the email in SharedPreferences:
        val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("user_email", null)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null && isTokenValid(context)
    }

    fun clearToken(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(TOKEN_KEY).apply()
    }


    suspend fun fetchUserIdByEmail(email: String, context: android.content.Context): Long? {
        return suspendCancellableCoroutine { cont ->
            val authService = ApiClient.getAuthService(context)
            authService.getAllUsers().enqueue(object : Callback<List<UserDTO>> {
                override fun onResponse(call: Call<List<UserDTO>>, response: Response<List<UserDTO>>) {
                    if (response.isSuccessful) {
                        // Find the user with the matching email (case-insensitive)
                        val user = response.body()?.find { it.email.equals(email, ignoreCase = true) }
                        cont.resume(user?.id)
                    } else {
                        cont.resume(null)
                    }
                }
                override fun onFailure(call: Call<List<UserDTO>>, t: Throwable) {
                    cont.resume(null)
                }
            })
        }
    }


    fun getToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(TOKEN_KEY, null)
    }
    private const val USERNAME_KEY = "USERNAME"

    fun saveUser(context: Context, username: String, userId: Long, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        prefs.putString(KEY_USERNAME, username)
        prefs.putLong(KEY_USER_ID, userId)
        prefs.putString(KEY_TOKEN, token)
        prefs.apply()
    }
    fun getUserEmail(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString("email", null)  // Ensure "email" is stored
    }

    fun getUserId(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getLong(KEY_USER_ID, -1L)
        return if (id != -1L) id else null
    }

    fun getUsername(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(USERNAME_KEY, null)
    }

}
