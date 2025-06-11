package com.example.recipeapp.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar

import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthService
import com.example.recipeapp.utils.UserDTO
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    context: Context,
    onSave: (username: String?, password: String?) -> Unit,
    recipes: MutableList<Recipe>
) {
    val passwordRegex = Regex("^(?=.*[A-Z])(?=.*\\d).{8,}$")

    var currentUsername by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val authService = ApiClient.getAuthService(context)
    var showAddOptionsModal by remember { mutableStateOf(false) }
    var isSheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Fetch subscription status
    LaunchedEffect(Unit) {
        authService.getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        when (user.subscriptionType?.uppercase() ?: "FREE") {
                            "FREE" -> {
                                plusUnlocked = false
                                proUnlocked = false
                            }
                            "PLUS" -> {
                                plusUnlocked = true
                                proUnlocked = false
                            }
                            "PRO" -> {
                                plusUnlocked = true
                                proUnlocked = true
                            }
                        }
                    }
                } else {
                    Log.e("EditProfileScreen", "Failed to fetch profile: ${response.code()} ${response.message()}")
                }
            }
            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("EditProfileScreen", "Error fetching profile: ${t.localizedMessage ?: "Unknown error"}")
            }
        })
    }

    // Fetch user details
    LaunchedEffect(Unit) {
        fetchUserDetails(
            context = context,
            onSuccess = { user ->
                currentUsername = user.username ?: ""
                email = user.email ?: ""
            },
            onFailure = {
                Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add options modal
    if (showAddOptionsModal) {
        AddOptionsModal(
            navController = navController,
            onDismiss = { showAddOptionsModal = false },
            onAddManually = { isSheetVisible = true }
        )
    }

    // Add recipe bottom sheet
    if (isSheetVisible) {
        val currentSubscriptionType = when {
            proUnlocked -> "PRO"
            plusUnlocked -> "PLUS"
            else -> "FREE"
        }
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                    isSheetVisible = false
                }
            },
            sheetState = sheetState
        ) {
            AddRecipeBottomSheetContent(
                navController = navController,
                onClose = {
                    isSheetVisible = false
                    coroutineScope.launch { sheetState.hide() }
                },
                recipes = recipes,
                subscriptionType = currentSubscriptionType
            )
        }
    }

    Scaffold(
        topBar = {
            // Using the same top bar style as in UserSearchScreen
            CookbookDetailTopBar(navController = navController, title = "LeGourmand")
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedTab = "profile",
                onTabSelected = { /* handle tab change if needed */ },
                onAddClick = { showAddOptionsModal = true } // Show Add Options Modal
            )
        }

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    // Header Text for Edit Page
                    Text(
                        text = "Personal Informations",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFAFAFAF),
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 27.dp)
                    )
                };
                item {
                    // Email Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Email Address",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFC0822D)
                                )
                            }
                        }
                    }
                }
                item {
                    // Username Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Username",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newUsername,
                                onValueChange = { newUsername = it },
                                label = { Text("New username", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(android.R.drawable.ic_menu_myplaces),
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                                    keyboardType = KeyboardType.Text // Adjusts the type of keyboard
                                ),
                                isError = errors.containsKey("username"),
                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFF876232),
                                    unfocusedBorderColor = Color.Gray,
                                    cursorColor = Color(0xFF876232)
                                )
                            )
                            errors["username"]?.let {
                                Text(text = it, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (newUsername.isEmpty()) {
                                        errors = mapOf("username" to "Username cannot be empty")
                                    } else {
                                        onSave(newUsername, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = newUsername.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF876232))
                            ) {
                                Text("Update Username", color = Color.White)
                            }
                        }
                    }
                }
                item {
                    // Password Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Password",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("New password", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(android.R.drawable.ic_lock_idle_lock),
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                                    keyboardType = KeyboardType.Text // Adjusts the type of keyboard
                                ),
                                isError = errors.containsKey("password"),
                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFF876232),
                                    unfocusedBorderColor = Color.Gray,
                                    cursorColor = Color(0xFF876232)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm new password", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                                    keyboardType = KeyboardType.Text // Adjusts the type of keyboard
                                ),
                                visualTransformation = PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(android.R.drawable.ic_lock_idle_lock),
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                },
                                isError = errors.containsKey("password"),
                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFF876232),
                                    unfocusedBorderColor = Color.Gray,
                                    cursorColor = Color(0xFF876232)
                                )
                            )
                            errors["password"]?.let {
                                Text(text = it, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    when {
                                        // Check if newPassword matches the pattern:
                                        !passwordRegex.matches(newPassword) -> {
                                            errors = mapOf("password" to "Password must contain at least 8 characters, one uppercase letter, and one number")
                                        }
                                        // Then check if passwords match:
                                        newPassword != confirmPassword -> {
                                            errors = mapOf("password" to "Passwords don't match")
                                        }
                                        else -> {
                                            // If all validations pass
                                            onSave(null, newPassword)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF876232))
                            ) {
                                Text("Change Password", color = Color.White)
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

fun updateUserProfile(
    context: Context,
    email: String,
    username: String?,
    password: String?,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val authService = ApiClient.getRetrofit(context).create(AuthService::class.java)
    val userDTO = UserDTO(
        email = email,
        username = username,
        password = password
    )

    authService.updateProfile(userDTO).enqueue(object : Callback<UserDTO> {
        override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
            if (response.isSuccessful) {
                onSuccess()
            } else {
                when (response.code()) {
                    400 -> Toast.makeText(context, "Invalid update data", Toast.LENGTH_SHORT).show()
                    401 -> Toast.makeText(context, "Authentication required", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                }
                onFailure()
            }
        }
        override fun onFailure(call: Call<UserDTO>, t: Throwable) {
            Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            onFailure()
        }
    })
}
