package com.example.recipeapp.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.ChallengeApi
import com.example.recipeapp.utils.UploadImageUtil
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileViewModel : ViewModel() {

    /**
     * Uploads an image, moderates it, and if approved, updates the user's avatar.
     * If moderation fails or the image is inappropriate, it returns null.
     */
    fun uploadAndModerateAvatarImage(
        context: Context,
        imageUri: Uri,
        onUploadComplete: (String?) -> Unit
    ) {
        viewModelScope.launch {
            // First, upload the image as a recipe image (i.e. without automatically updating the avatar)
            UploadImageUtil.uploadImage(
                context,
                imageUri,
                isAvatar = false, // Do not update avatar automatically
                onSuccess = { imageUrl ->
                    Log.d("ProfileViewModel", "Uploaded image URL: $imageUrl")
                    // Call moderation API using your ChallengeApi
                    val challengeApi = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)
                    val call = challengeApi.moderateImage(imageUrl)
                    call.enqueue(object : Callback<Map<String, Boolean>> {
                        override fun onResponse(
                            call: Call<Map<String, Boolean>>,
                            response: Response<Map<String, Boolean>>
                        ) {
                            if (response.isSuccessful) {
                                val body = response.body()
                                val isAppropriate = body?.get("isAppropriate") ?: false
                                Log.d("ProfileViewModel", "Moderation result: $isAppropriate")
                                if (!isAppropriate) {
                                    onUploadComplete(null)
                                } else {
                                    // If approved, update the avatar via the updateAvatar endpoint.
                                    val authService = ApiClient.getAuthService(context)
                                    val request = mapOf("imageUri" to imageUrl)
                                    authService.updateAvatar(request).enqueue(object : Callback<String> {
                                        override fun onResponse(
                                            call: Call<String>,
                                            response: Response<String>
                                        ) {
                                            if (response.isSuccessful) {
                                                onUploadComplete(imageUrl)
                                            } else {
                                                onUploadComplete(null)
                                            }
                                        }
                                        override fun onFailure(call: Call<String>, t: Throwable) {
                                            onUploadComplete(null)
                                        }
                                    })
                                }
                            } else {
                                onUploadComplete(null)
                            }
                        }
                        override fun onFailure(call: Call<Map<String, Boolean>>, t: Throwable) {
                            onUploadComplete(null)
                        }
                    })
                },
                onError = { errorMsg ->
                    Log.e("ProfileViewModel", "Upload failed: $errorMsg")
                    onUploadComplete(null)
                }
            )
        }
    }
}
