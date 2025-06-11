package com.example.recipeapp.utils


import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.recipeapp.network.AppwriteClient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.InputFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.FileOutputStream
import java.io.InputStream
import java.io.File

object UploadRecipeImageUtil {
    private const val BUCKET_ID = "67b7edd400131c188c97" //  Appwrite Storage Bucket ID

    suspend fun uploadRecipeImage(
        context: Context,
        recipeId: Long,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        AppwriteClient.initClient(context)
        val storage = AppwriteClient.storage

        try {
            val tempFile = createTempFileFromUri(context, imageUri)
            val inputFile = InputFile.fromFile(tempFile)

            withContext(Dispatchers.IO) {
                val uploadedFile = storage.createFile(
                    bucketId = BUCKET_ID,
                    fileId = "unique()",
                    file = inputFile
                )

                val imageUrl = getPublicUrl(uploadedFile.id)

                // Convert file to MultipartBody.Part for Retrofit
                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)

                val recipeService = ApiClient.getRecipeService(context)

                recipeService.updateRecipeImage(recipeId, multipartBody)
                    .enqueue(object : Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            if (response.isSuccessful) {
                                Log.d("UploadRecipeImage", "Recipe image updated: $imageUrl")
                                onSuccess(imageUrl)
                            } else {
                                Log.e("UploadRecipeImage", "Backend update failed: ${response.code()} - ${response.errorBody()?.string()}")
                                onError("Failed to update recipe image in backend")
                            }
                        }

                        override fun onFailure(call: Call<String>, t: Throwable) {
                            Log.e("UploadRecipeImage", "Error updating recipe image", t)
                            onError(t.localizedMessage ?: "Unknown error")
                        }
                    })
            }
        } catch (e: AppwriteException) {
            Log.e("UploadRecipeImage", "Upload failed", e)
            onError(e.message ?: "Unknown error")
        }
    }


    private fun createTempFileFromUri(context: Context, uri: Uri): File {
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
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
    suspend fun uploadImageOnly(
        context: Context,
        imageUri: Uri
    ): String = withContext(Dispatchers.IO) {
        // Initialize Appwrite client
        AppwriteClient.initClient(context)
        val storage = AppwriteClient.storage

        // Create temporary file from URI
        val tempFile = createTempFileFromUri(context, imageUri)
        val inputFile = InputFile.fromFile(tempFile)
        // Upload file to Appwrite Storage with auto-generated ID
        val uploadedFile = storage.createFile(
            bucketId = BUCKET_ID,
            fileId = "unique()",
            file = inputFile
        )
        // Return the public URL
        getPublicUrl(uploadedFile.id)
    }

}
