package com.example.recipeapp.utils

import androidx.compose.foundation.layout.Column

import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // Make sure you have this import
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.network.AppwriteClient
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.AuthService

import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO
import com.example.recipeapp.viewmodels.ProfileViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
@Composable
fun ProfileHeaderCircle(
    author: UserDTO,
    isOwnProfile: Boolean,
    isFollowing: Boolean = false,
    onFollowToggle: () -> Unit = {},
            refreshProfile: () -> Unit = {},
    onAvatarClick: (() -> Unit)? = null
) {
    // Locally selected avatar URI (if the user picked a new image)
    var avatarUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val viewModel = ViewModelProvider(context as ViewModelStoreOwner).get(ProfileViewModel::class.java)

    // Launcher to pick an image from storage.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { pickedUri: Uri? ->
            if (pickedUri != null) {
                avatarUri = pickedUri
                Log.d("ProfileHeaderCircle", "User picked image: $pickedUri")
                // Call our ViewModel's function to upload and moderate the avatar image.
                viewModel.uploadAndModerateAvatarImage(context, pickedUri) { newAvatarUrl ->
                    if (newAvatarUrl != null) {
                        Log.d("ProfileHeaderCircle", "Avatar updated successfully: $newAvatarUrl")
                        refreshProfile() // Refresh profile to show the updated avatar.
                    } else {
                        Log.e("ProfileHeaderCircle", "Image rejected or upload failed due to moderation")
                        Toast.makeText(
                            context,
                            "Selected image is inappropriate or upload failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    )

    // If you need to perform side effects whenever 'author.imageUri' changes:
    LaunchedEffect(author.imageUri) {
        // If there's a valid URI from the backend and we haven't picked a local image, use it.
        if (!author.imageUri.isNullOrEmpty() && avatarUri == null) {
            Log.d("ProfileHeaderCircle", "Using backend imageUri: ${author.imageUri}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar container
        // Wrap the avatar container in a clickable modifier if an onAvatarClick lambda is provided.
        Box(
            modifier = Modifier
                .size(80.dp)
                .then(
                    if (onAvatarClick != null) Modifier.clickable { onAvatarClick() } else Modifier
                )
        ) {
            when {
                avatarUri != null -> {
                    Image(
                        painter = rememberAsyncImagePainter(model = avatarUri),
                        contentDescription = "Profile Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                !author.imageUri.isNullOrBlank() -> {
                    Image(
                        painter = rememberAsyncImagePainter(model = author.imageUri),
                        contentDescription = "Profile Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                else -> {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = author.username?.take(2)?.uppercase() ?: "NN",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            if (isOwnProfile) {
                IconButton(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.White, shape = CircleShape)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Username
        Text(
            text = author.username ?: "Unknown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF878787)
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Followers & Following count
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${author.followerCount ?: 0}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF878787)
                )
                Text(
                    text = "Followers",
                    fontSize = 14.sp,
                    color = Color(0xFF878787)
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${author.followingCount ?: 0}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF878787)
                )
                Text(
                    text = "Following",
                    fontSize = 14.sp,
                    color = Color(0xFF878787)
                )
            }
        }

        // If this is not the user's profile, show Follow/Unfollow button
        if (!isOwnProfile) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onFollowToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) Color.Gray else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isFollowing) "Unfollow" else "Follow",
                    color = Color.White
                )
            }
        }
    }
}
