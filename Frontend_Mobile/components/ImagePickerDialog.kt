package com.example.recipeapp.components

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.recipeapp.utils.saveBitmapAndGetUri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerDialog(
    context: Context,
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {

    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && capturedImageUri != null) {
            onImageSelected(capturedImageUri!!)
        }
    }
    var cameraPermissionGranted by remember {
        mutableStateOf(sharedPreferences.getBoolean("camera_permission_granted", false))
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        cameraPermissionGranted = isGranted
    }
    // Launchers for gallery and camera
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }


    // Function to create and return a file Uri
    fun createImageUri(context: Context): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "RecipeApp")

        // Ensure the directory exists
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val imageFile = File(storageDir, "IMG_${timestamp}.jpg")

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Ensure this matches the authority in AndroidManifest.xml
            imageFile
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(Color(0xFF1C1917), shape = RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Edit image", fontSize = 20.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "You can upload your own image for the recipe.",
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Icon(Icons.Default.Image, contentDescription = "Select Image")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select image")
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Take a picture using the camera
            // Take a picture using the camera (Auto Permission Handling)
            // Take a picture using the camera (Auto Permission Handling)
            Button(
                onClick = {
                    if (cameraPermissionGranted) {
                        val uri = createImageUri(context)
                        if (uri != null) {
                            capturedImageUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Take Picture")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Take picture")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "By uploading an image, you agree that you have the right to publish the image and allow LeGourmand to store and distribute the image.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
private fun launchCamera(context: Context, onImageSelected: (Uri) -> Unit) {
    val capturedImageUri = createImageUri(context)
    if (capturedImageUri != null) {
        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                onImageSelected(capturedImageUri)
            }
        }
        cameraLauncher.launch(capturedImageUri)
    } else {
        Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show()
    }
}

private fun createImageUri(context: Context): Uri? {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "RecipeApp")

    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }

    val imageFile = File(storageDir, "IMG_${timestamp}.jpg")

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}