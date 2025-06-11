package com.example.recipeapp.utils


import android.content.Context
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.*

data class UserDTO(
    val id: Long? = null,
    val email: String,
    val username: String? = null,
    val password: String? = null,
    val role: String? = null,
    val subscriptionType: String? = "FREE",
    val subscriptionExpiry: String? = null,
    val followerCount: Int? = null,
    val followingCount: Int? = null,
    val imageUri: String? = null,
    // Optionally, include lists of IDs:
    val followerIds: List<Long>? = null,
    val followingIds: List<Long>? = null,
    val isFollowed: Boolean? = null,
    var badges: Map<String, Int> = emptyMap()
)
data class AuthResponseDTO(val accessToken: String)
data class ForgotPasswordRequest(val email: String)
data class VerifyResetCodeRequest(val email: String, val code: String)
data class UpdatePasswordRequest(val email: String, val newPassword: String)

interface AuthService {
    @POST("api/auth/register")
    fun register(@Body user: UserDTO): Call<UserDTO>


    @GET("users/by-username/{username}")
    fun getUserByUsername(@Path("username") username: String): Call<UserDTO>
    fun getUserRole(context: Context): String? {
        // Assume you save the role in SharedPreferences when the user logs in.
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("user_role", "user")  // default to "user" if not found
    }
    @POST("api/users/awardBadge")
    fun awardBadge(
        @Query("userEmail") userEmail: String,
        @Query("badge") badge: String
    ): Call<String>

    @PUT("api/auth/profile/avatar")
    fun updateAvatar(@Body request: Map<String, String>): Call<String>
    @POST("api/auth/login")
    fun login(@Body user: UserDTO): Call<AuthResponseDTO>
    @GET("api/auth/isFollowing/{userId}")
    fun isFollowingUser(@Path("userId") userId: Long): Call<Boolean>
    @DELETE("api/auth/profile/avatar")
    fun deleteAvatar(): Call<Void>
    @GET("api/auth/profile")
    fun getProfile(): Call<UserDTO>

    @GET("api/auth/{id}") // New endpoint
    fun getUserById(@Path("id") userId: Long): Call<UserDTO>

    @GET("api/auth/{id}")
    suspend fun getUserByIdSuspend(@Path("id") userId: Long): UserDTO
    @DELETE("api/auth/{id}")
    fun deleteUser(@Path("id") id: Long): Call<Void>
    @GET("api/auth")
    fun getAllUsers(): Call<List<UserDTO>>
    @POST("/api/feedback")
    fun submitFeedback(@Body feedback: FeedbackDTO): Call<Void>
    // "api/auth/follow/{userId}"
    @POST("api/auth/follow/{userId}")
    fun followUser(@Path("userId") userId: Long): Call<Void>
    @GET("api/auth/verify-email")
    fun verifyEmail(@Query("token") token: String): Call<String>
    @Multipart
    @POST("api/auth/profile/avatar")
    fun uploadAvatar(
        @Part avatar: MultipartBody.Part
    ): Call<String>

    @POST("api/reviewReports/{reviewId}")
    fun reportReview(
        @Path("reviewId") reviewId: Long,
        @Body reason: String
    ): Call<ReviewReportDTO>
    // "api/auth/unfollow/{userId}"
    @POST("api/auth/unfollow/{userId}")
    fun unfollowUser(@Path("userId") userId: Long): Call<Void>

    // Updated subscribeNewsletter without body
    @POST("api/newsletter/subscribe")
    fun subscribeNewsletter(): Call<Void>

    // New endpoint to check subscription status
    @GET("api/newsletter/isSubscribed")
    fun isSubscribed(): Call<Boolean>

    // (Optional) Unsubscribe endpoint
    @POST("api/newsletter/unsubscribe")
    fun unsubscribeNewsletter(): Call<Void>

    @PUT("api/auth/profile")
    fun updateProfile(@Body user: UserDTO): Call<UserDTO>

    @PUT("api/auth/subscription")
    suspend fun updateSubscription(@Body subscriptionRequest: SubscriptionRequest): Response<Void>

    @POST("/api/auth/googleLogin")
    fun loginWithGoogle(@Body body: Map<String, String>): Call<AuthResponseDTO>

    @POST("api/auth/forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<String>
    @POST("api/auth/resend-otp")
    fun resendOtp(@Query("email") email: String): Call<Void>

    @POST("api/auth/forgot-password/verify")
    fun verifyResetCode(@Body request: VerifyResetCodeRequest): Call<String>

    @POST("api/auth/forgot-password/update")
    fun updatePasswordAfterVerification(@Body request: UpdatePasswordRequest): Call<String>

    @POST("api/auth/verify-otp")
    fun verifyOtp(@Body request: OtpRequest): Call<String>

}
