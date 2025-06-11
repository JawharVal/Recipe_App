package com.example.recipeapp.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.recipeapp.R
import com.example.recipeapp.ui.theme.RecipeAppTheme
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.AuthResponseDTO
import com.example.recipeapp.utils.AuthService
import com.example.recipeapp.utils.UserDTO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    RecipeAppTheme {
        LoginScreen(navController = rememberNavController())
    }
}


@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    val authService = ApiClient.getRetrofit(context).create(AuthService::class.java)

    val auth = FirebaseAuth.getInstance()

// Configure Google Sign-In
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("516900387761-m4jveee923usef9enhdfrd5btct2sm97.apps.googleusercontent.com")
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

    val scrollState = rememberScrollState() // Create scroll state


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image with a gradient overlay
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x2D82CE17), Color(0x2AAAC289)),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        )

        // Main Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier

                .verticalScroll(scrollState) // Enable scrolling
                .padding(16.dp)
                .background(
                    color = Color(0xE8FFFFFF),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
                .align(Alignment.Center)
        ) {
            // Logo Icon with a shadow
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                modifier = Modifier
                    .offset(y = (-10).dp)
                    .scale(scale)
                    .shadow(8.dp, shape = RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF66BB6A), Color(0xFF43A047))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_loogo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            Text("Welcome back Chef!", fontSize = 28.sp, color = Color.Gray ,  textAlign = TextAlign.Center,style = LocalTextStyle.current.copy(lineHeight = 28.sp)  )
            // Title and Subtitle with a subtle drop shadow for effect
            Text("Glad to see you again", fontSize = 20.sp, color = Color.Gray ,  textAlign = TextAlign.Center,style = LocalTextStyle.current.copy(lineHeight = 28.sp)  )

            Spacer(modifier = Modifier.height(16.dp))



            Spacer(modifier = Modifier.height(23.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Gray), // Set the text color to white
                label = { Text("Email Address", color = Color.Gray) },
                placeholder = { Text(" ") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.sym_action_email),
                        contentDescription = "Email Icon",
                        tint = Color(0xFF878787)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color.LightGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )


            // ---- Password Field ----
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Gray),
                label = { Text("Password", color = Color.Gray) },
                placeholder = { Text(" ") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_lock_idle_lock),
                        contentDescription = "Lock Icon",
                        tint = Color(0xFF878787)
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color.LightGray
                ),
                modifier = Modifier.fillMaxWidth()
            )
            // "Forgot Password?" link
            Text(
                text = "Forgot Password?",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { navController.navigate("forgotPassword") }
                    .padding(top = 8.dp)
            )


            Spacer(modifier = Modifier.height(32.dp))

            // Login Button with gradient background and shadow

            // Login Button Logic
            Button(
                onClick = {
                    val userRequest = UserDTO(
                        email = email.text,
                        username = null,
                        password = password.text
                    )
                    authService.login(userRequest).enqueue(object : Callback<AuthResponseDTO> {
                        override fun onResponse(
                            call: Call<AuthResponseDTO>,
                            response: Response<AuthResponseDTO>
                        ) {
                            if (response.isSuccessful) {
                                val authResponse = response.body()
                                val token = authResponse?.accessToken
                                if (token != null) {
                                    // Save token
                                    AuthPreferences.saveToken(context, token)
                                    // Now fetch the user profile with the new token
                                    val authServiceWithToken = ApiClient.getRetrofit(context)
                                        .create(AuthService::class.java)
                                    authServiceWithToken.getProfile()
                                        .enqueue(object : Callback<UserDTO> {
                                            override fun onResponse(
                                                call: Call<UserDTO>,
                                                profileResponse: Response<UserDTO>
                                            ) {
                                                if (profileResponse.isSuccessful) {
                                                    val userProfile = profileResponse.body()
                                                    if (userProfile != null && userProfile.id != null) {
                                                        // Save user info
                                                        AuthPreferences.saveUser(
                                                            context,
                                                            userProfile.username ?: "",
                                                            userProfile.id,
                                                            token
                                                        )
                                                        // Navigate
                                                        navController.navigate("home") {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to retrieve user ID.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } else {
                                                    // Only read errorBody in the error case:
                                                    val errorMsg = profileResponse.errorBody()
                                                        ?.string()
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to retrieve user profile: $errorMsg",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }

                                            override fun onFailure(
                                                call: Call<UserDTO>,
                                                t: Throwable
                                            ) {
                                                Toast.makeText(
                                                    context,
                                                    "Error fetching user profile: ${t.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        })
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Login failed Token is null.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                // Not successful => read errorBody here
                                val errorMsg = response.errorBody()?.string()
                                Toast.makeText(
                                    context,
                                    "Login failed, Email or Password incorrect.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<AuthResponseDTO>, t: Throwable) {
                            Toast.makeText(
                                context,
                                "Login error: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                // Use a Box to draw the gradient background with shadow and center the text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(8.dp, shape = RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Login", fontSize = 18.sp, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Register Here",
                color = Color(0xFF4CAF50),
                fontSize = 16.sp,
                modifier = Modifier.clickable { navController.navigate("register") }
            )

            Spacer(modifier = Modifier.height(24.dp))


            // Rating section with centered icons and text
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_star), // Replace with star icon
                        contentDescription = "Star",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))

            }
            Text("Loved by 43,500 cooks worldwide", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        println("Google Sign-In token: ${account.idToken}")

                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                val firebaseUser = auth.currentUser
                                println("Success! Firebase user ID: ${firebaseUser?.uid}")

                                // 1) Call your backend to log in with Google
                                val googleIdToken = account.idToken
                                if (googleIdToken != null) {
                                    val body = mapOf("idToken" to googleIdToken)
                                    authService.loginWithGoogle(body).enqueue(object : Callback<AuthResponseDTO> {
                                        override fun onResponse(call: Call<AuthResponseDTO>, response: Response<AuthResponseDTO>) {
                                            if (response.isSuccessful) {
                                                val token = response.body()?.accessToken
                                                if (token != null) {
                                                    // 2) Save token
                                                    AuthPreferences.saveToken(context, token)

                                                    // 3) Now fetch user profile
                                                    val authServiceWithToken = ApiClient.getAuthService(context)
                                                    authServiceWithToken.getProfile().enqueue(object : Callback<UserDTO> {
                                                        override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                                                            if (response.isSuccessful) {
                                                                val userProfile = response.body()
                                                                if (userProfile != null) {
                                                                    // 4) Save user ID, username, etc.
                                                                    AuthPreferences.saveUser(
                                                                        context,
                                                                        userProfile.username ?: "",
                                                                        userProfile.id ?: 0L,
                                                                        token
                                                                    )
                                                                    // 5) Finally, navigate
                                                                    navController.navigate("home") {
                                                                        popUpTo("login") { inclusive = true }
                                                                    }
                                                                } else {
                                                                    println("Server returned null user profile.")
                                                                    Toast.makeText(context, "No user profile found.", Toast.LENGTH_SHORT).show()
                                                                }
                                                            } else {
                                                                println("Profile call failed: ${response.code()} - ${response.message()}")
                                                                Toast.makeText(context, "Failed to fetch user profile.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                        override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                                                            println("Profile network error: ${t.message}")
                                                            Toast.makeText(context, "Profile network error.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    })

                                                } else {
                                                    println("Server responded with null token.")
                                                    Toast.makeText(context, "Google login: No token.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                println("Google login failed in backend: ${response.code()} - ${response.message()}")
                                                Toast.makeText(
                                                    context,
                                                    "Google login failed in backend: ${response.code()}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        override fun onFailure(call: Call<AuthResponseDTO>, t: Throwable) {
                                            println("Network error: ${t.message}")
                                            Toast.makeText(
                                                context,
                                                "Google login network error: ${t.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                                } else {
                                    println("Error: Google ID token is null.")
                                }
                            } else {
                                println("Error: ${authTask.exception?.message}")
                                Toast.makeText(context, "Firebase Auth failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        println("Google Sign-In error: ${e.message}")
                        Toast.makeText(context, "Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    println("Google Sign-In canceled or failed")
                    Toast.makeText(context, "Sign-In canceled", Toast.LENGTH_SHORT).show()
                }
            }


            Spacer(modifier = Modifier.height(16.dp))
            // Register Link

            Button(
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    launcher.launch(signInIntent) // Use launcher to handle the result
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDB4437), // Google red color
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Assuming you have a vector asset for the Google logo named ic_google in your resources
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))


            TermsPrivacyText()
        }
    }
}

@Composable
fun SplashScreen(navController: NavController, context: Context) {
    var checkedAuth by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!checkedAuth) {
            checkedAuth = true
            val token = AuthPreferences.getToken(context)

            if (token == null || !AuthPreferences.isTokenValid(context)) {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }
}

@Composable
fun TermsPrivacyText() {
    // State variables for dialog visibility and content
    var showDialog by remember { mutableStateOf(false) }
    var dialogContent by remember { mutableStateOf("") }

    // Build the annotated string with clickable parts
    val annotatedText = buildAnnotatedString {
        append("By registering, you agree to our ")

        // Annotate the Privacy Policy text
        pushStringAnnotation(tag = "privacy", annotation = "privacy")
        withStyle(style = SpanStyle(color = Color(0xFF737373), textDecoration = TextDecoration.Underline)) {
            append("Privacy Policy")
        }
        pop()

        append(" and ")

        // Annotate the Terms of Use text
        pushStringAnnotation(tag = "terms", annotation = "terms")
        withStyle(style = SpanStyle(color = Color(0xFF737373), textDecoration = TextDecoration.Underline)) {
            append("Terms of Use")
        }
        pop()

        append(".")
    }

    // Display the clickable text
    ClickableText(
        text = annotatedText,
        onClick = { offset ->
            // Check if the click occurred on the Privacy Policy text
            val privacyAnnotation = annotatedText.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                .firstOrNull()
            if (privacyAnnotation != null) {
                dialogContent = """
                    We are committed to protecting your privacy. This Privacy Policy explains how we collect, use, and safeguard your information.
                    
                    1. Information We Collect:
                    - Personal details (name, email) provided during registration.
                                    
                    2. How We Use Your Data:
                    - To provide and improve our services.
                    - To personalize content and user experience.
                    - To comply with legal obligations.
                    
                    3. Data Protection & Security:
                    - Your data is securely stored and protected from unauthorized access.
                    - We do not sell or share your personal information with third parties without consent.
                """.trimIndent()
                showDialog = true
                return@ClickableText
            }

            // Check if the click occurred on the Terms of Use text
            val termsAnnotation = annotatedText.getStringAnnotations(tag = "terms", start = offset, end = offset)
                .firstOrNull()
            if (termsAnnotation != null) {
                dialogContent = """
                    By using our application, you agree to comply with these Terms of Service.
                    
                    1. Acceptance of Terms:
                    - Your access and use of the app is subject to these terms.
                    - If you do not agree, please do not use the app.
                    
                    2. User Responsibilities:
                    - You must not use our services for any illegal activities.
                    - You are responsible for maintaining the confidentiality of your login credentials.
                    
                    3. Limitations of Liability:
                    - We are not liable for any damages resulting from the use of our services.
                    - We reserve the right to modify or terminate the service at any time.
                    
                    4. Changes to Terms:
                    - We may update these terms occasionally. Continued use of the app constitutes acceptance of any changes.
                """.trimIndent()
                showDialog = true
            }
        },
        modifier = Modifier
            .fillMaxWidth() // Ensures the text occupies the full width
            .padding(horizontal = 16.dp),
        style = TextStyle(
            textAlign = TextAlign.Center, // Center aligns the text
            fontSize = 12.sp,
            color = Color.Gray
        )
    )

    // Show the dialog if showDialog is true
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Details") },
            text = { Text(dialogContent) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
