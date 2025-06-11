package com.example.recipeapp.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.File
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.recipeapp.network.AppwriteClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.FileOutputStream
import java.io.InputStream

object UploadImageUtil {
    private const val BUCKET_ID = "67b7edd400131c188c97" //  Appwrite Storage Bucket ID

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        isAvatar: Boolean, // true for avatar updates, false for recipe images.
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        AppwriteClient.initClient(context)
        val storage = AppwriteClient.storage

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create a temporary file from the selected URI
                val tempFile = createTempFileFromUri(context, imageUri)
                val inputFile = InputFile.fromFile(tempFile)

                // Upload the file to Appwrite
                val uploadedFile = storage.createFile(
                    bucketId = BUCKET_ID,
                    fileId = "unique()", // Generates a unique file ID
                    file = inputFile
                )

                // Build the public URL from the uploaded file's ID
                val imageUrl = getPublicUrl(uploadedFile.id)

                withContext(Dispatchers.Main) {
                    if (isAvatar) {
                        // For avatars, call the updateAvatar endpoint.
                        val authService = ApiClient.getAuthService(context)
                        val request = mapOf("imageUri" to imageUrl)
                        authService.updateAvatar(request).enqueue(object : Callback<String> {
                            override fun onResponse(call: Call<String>, response: Response<String>) {
                                if (response.isSuccessful) {
                                    Log.d("UploadImage", "Avatar updated in backend: $imageUrl")
                                    onSuccess(imageUrl)
                                } else {
                                    Log.e("UploadImage", "Failed to update avatar: ${response.message()}")
                                    onError("Failed to update avatar in backend")
                                }
                            }
                            override fun onFailure(call: Call<String>, t: Throwable) {
                                Log.e("UploadImage", "Error updating avatar in backend", t)
                                onError(t.localizedMessage ?: "Unknown error")
                            }
                        })
                    } else {
                        // For recipe images, skip updating the avatar and just return the URL.
                        onSuccess(imageUrl)
                    }
                }
            } catch (e: AppwriteException) {
                Log.e("UploadImage", "Upload failed", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }



    private fun createTempFileFromUri(context: Context, uri: Uri): java.io.File {
        val file = java.io.File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)

        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        return file
    }

    fun getPublicUrl(fileId: String): String {
        return "https://cloud.appwrite.io/v1/storage/buckets/$BUCKET_ID/files/$fileId/view?project=67b7ea4e000d06d8d51a"
    }
}
