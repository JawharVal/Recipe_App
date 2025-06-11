// File: SubmitChallengeScreen.kt
package com.example.recipeapp.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.ChallengeApi
import com.example.recipeapp.utils.ChallengeDTO
import com.example.recipeapp.utils.ChallengeRepository
import com.example.recipeapp.utils.ChallengesViewModelFactory
import com.example.recipeapp.viewmodel.ChallengesViewModel
import java.util.Calendar

@Composable
fun SubmitChallengeScreen(navController: NavController) {
    val context = LocalContext.current

    // Initialize API and ViewModel
    val challengeApi: ChallengeApi = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)
    val repository = ChallengeRepository(challengeApi)
    val factory = ChallengesViewModelFactory(repository)
    val viewModel: ChallengesViewModel = viewModel(factory = factory)

    // Form State
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var pointsSliderValue by remember { mutableStateOf(10f) }
    var maxSubmissions by remember { mutableStateOf("") } // as String for input
    val maxSubmissionsInt = maxSubmissions.toIntOrNull() ?: 1

    // Image State
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // DatePicker State
    var showDatePicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                // Format selected date as "YYYY-MM-DD"
                val formattedMonth = (selectedMonth + 1).toString().padStart(2, '0')
                val formattedDay = selectedDayOfMonth.toString().padStart(2, '0')
                deadline = "$selectedYear-$formattedMonth-$formattedDay"
                showDatePicker = false
            },
            year,
            month,
            day
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { pickedUri: Uri? ->
            if (pickedUri != null) {
                selectedImageUri = pickedUri
                isUploading = true
                Log.d("SubmitChallengeScreen", "User picked image: $pickedUri")
                viewModel.uploadProfileImage(context, pickedUri) { uploadedUrl ->
                    isUploading = false
                    if (uploadedUrl != null) {
                        uploadedImageUrl = uploadedUrl
                        Log.d("SubmitChallengeScreen", "Successfully uploaded: $uploadedUrl")
                    } else {
                        Log.e("SubmitChallengeScreen", "Failed to upload image.")
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Submit a Challenge") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Image Picker Button
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedImageUri == null) "Select Image" else "Change Selected Image")
            }
            // Display Selected/Uploaded Image
            if (selectedImageUri != null || uploadedImageUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ChallengeImage(
                    imageUriString = uploadedImageUrl ?: selectedImageUri.toString(),
                    isUploading = isUploading
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Deadline Field (opens DatePicker on click)
            OutlinedTextField(
                value = deadline,
                onValueChange = {},
                label = { Text("Deadline") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false,
                trailingIcon = {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Pick Date")
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Points Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Points: ${pointsSliderValue.toInt()}", modifier = Modifier.align(Alignment.CenterHorizontally))
                Slider(
                    value = pointsSliderValue,
                    onValueChange = { pointsSliderValue = it },
                    valueRange = 1f..100f,
                    steps = 98,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Max Submissions Input
            OutlinedTextField(
                value = maxSubmissions,
                onValueChange = { input -> maxSubmissions = input.filter { it.isDigit() } },
                label = { Text("Max Submissions per User") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Submit Challenge Button (only enabled when required fields are set)
            Button(
                onClick = {
                    val points = pointsSliderValue.toInt()
                    Log.d("SubmitFeaturedChallenge", "Submitting challenge: $title, deadline: $deadline")
                    // Create new challenge DTO and mark it as featured
                    val newChallenge = ChallengeDTO(
                        id = 0L,
                        title = title,
                        description = description,
                        imageUrl = uploadedImageUrl ?: "",
                        deadline = deadline,
                        points = points,
                        active = true,
                        maxSubmissions = maxSubmissionsInt,
                        featured = false // Mark as featured challenge
                    )
                    viewModel.createChallenge(newChallenge) {
                        // After submission, navigate back or show a success message.
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && deadline.isNotBlank() && uploadedImageUrl != null
            ) {
                Text("Submit")
            }
        }
    }
}

@Composable
fun ChallengeImage(imageUriString: String, isUploading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Image(
            painter = rememberImagePainter(data = imageUriString),
            contentDescription = "Challenge Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isUploading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}
