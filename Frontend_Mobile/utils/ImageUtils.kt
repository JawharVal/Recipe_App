package com.example.recipeapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

fun saveBitmapAndGetUri(context: Context, bitmap: Bitmap): Uri? {
    // Create a file in the cache directory
    val file = File(context.cacheDir, "temp_image.png")
    try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // Save the bitmap to the file
        }
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}




fun Bitmap.toUri(context: Context): Uri {
    val file = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
    file.outputStream().use {
        this.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}
