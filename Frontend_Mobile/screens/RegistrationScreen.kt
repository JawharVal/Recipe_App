package com.example.recipeapp.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recipeapp.R
import com.example.recipeapp.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 1) Validation Helpers
fun isEmailValid(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
fun isPasswordStrong(password: String): Boolean {
    val passwordPattern = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+\$).{8,}\$")
    return passwordPattern.matches(password)
}

private val CreamBackground = Color(0xFFF8F3E9)
private val EarthyBrown = Color(0xFF4B3B2F)
private val WarmOrange = Color(0xFFE69542)
private val SageGreen = Color(0xFF8DA45D)
private val ErrorRed = Color(0xFFD32F2F)
// 3) Registration Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavController) {

    // A) Registration input states
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var confirmPassword by remember { mutableStateOf(TextFieldValue("")) }

    // B) Error states
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }

    // C) OTP dialog toggles
    var showOtpDialog by remember { mutableStateOf(false) }
    var registeredEmail by remember { mutableStateOf("") }

    // D) Context & AuthService
    val context = LocalContext.current
    val authService = ApiClient.getRetrofit(context).create(AuthService::class.java)

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Background Image ---
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // --- Foreground Card/Column ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(24.dp)
                .align(Alignment.Center)
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_loogo),
                contentDescription = "App Logo",
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text("Hello! Register to get started", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(24.dp))

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Gray),
                label = { Text("Name", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = "Name Icon",
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

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = if (it.text.isNotEmpty() && !isEmailValid(it.text)) {
                        "Invalid email format"
                    } else ""
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Gray),
                label = { Text("Email", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.sym_action_email),
                        contentDescription = "Email Icon",
                        tint = Color(0xFF878787)
                    )
                },
                isError = emailError.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (emailError.isEmpty()) Color(0xFF4CAF50) else Color.Red,
                    unfocusedBorderColor = if (emailError.isEmpty()) Color.LightGray else Color.Red
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
            if (emailError.isNotEmpty()) {
                Text(emailError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = if (it.text.isNotEmpty() && !isPasswordStrong(it.text)) {
                        "Password must be 8+ chars, include uppercase, lowercase, and number"
                    } else ""
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Gray),
                label = { Text("Password", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_lock_idle_lock),
                        contentDescription = "Password Icon",
                        tint = Color(0xFF878787)
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (passwordError.isEmpty()) Color(0xFF4CAF50) else Color.Red,
                    unfocusedBorderColor = if (passwordError.isEmpty()) Color.LightGray else Color.Red
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
            if (passwordError.isNotEmpty()) {
                Text(passwordError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = if (it.text.isNotEmpty() && it.text != password.text) {
                        "Passwords do not match"
                    } else ""
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Gray),
                label = { Text("Confirm Password", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_lock_idle_lock),
                        contentDescription = "Confirm Password Icon",
                        tint = Color(0xFF878787)
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmPasswordError.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (confirmPasswordError.isEmpty()) Color(0xFF4CAF50) else Color.Red,
                    unfocusedBorderColor = if (confirmPasswordError.isEmpty()) Color.LightGray else Color.Red
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
            if (confirmPasswordError.isNotEmpty()) {
                Text(confirmPasswordError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- Spacing ---
            Spacer(modifier = Modifier.height(24.dp))

            // Register Button
            Button(
                onClick = {
                    // Clear errors
                    emailError = ""
                    passwordError = ""
                    confirmPasswordError = ""

                    val validEmail = isEmailValid(email.text)
                    val validPassword = isPasswordStrong(password.text)
                    val matchingPassword = (password.text == confirmPassword.text)

                    if (!validEmail) {
                        emailError = "Invalid email format"
                    }
                    if (!validPassword) {
                        passwordError = "Password must be 8+ chars, include uppercase, lowercase, and number"
                    }
                    if (!matchingPassword) {
                        confirmPasswordError = "Passwords do not match"
                    }

                    if (validEmail && validPassword && matchingPassword) {
                        val user = UserDTO(email = email.text, username = name.text, password = password.text)
                        authService.register(user).enqueue(object : Callback<UserDTO> {
                            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                                if (response.isSuccessful) {
                                    // New user => open OTP dialog
                                    registeredEmail = email.text
                                    showOtpDialog = true
                                } else {
                                    val statusCode = response.code()
                                    val errorBody = response.errorBody()?.string().orEmpty()

                                    // If backend says "unverified" => re-send OTP scenario
                                    if (statusCode == 409) {
                                        // Check if the error message explicitly indicates unverified user
                                        if (errorBody.contains("not verified", ignoreCase = true)) {
                                            // Unverified user scenario → show OTP dialog
                                            Toast.makeText(
                                                context,
                                                "We resent your verification code. Please verify!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            registeredEmail = email.text
                                            showOtpDialog = true
                                        } else if (errorBody.contains("already in use", ignoreCase = true)) {
                                            // Verified user already exists → show error only
                                            emailError = "Email is already in use."
                                        } else {
                                            emailError = "Registration failed: $errorBody"
                                        }
                                    } else {
                                        emailError = "Registration failed: $errorBody"
                                    }

                                }
                            }

                            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                                emailError = "Registration error: ${t.message}"
                            }
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
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
                    Text("Register", fontSize = 18.sp, color = Color.White)
                }
            }

            // "Already have an account?"
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Text("Already have an account?", color = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Login",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.clickable {
                        navController.navigate("login")
                    }
                )
            }
        }

        if (showOtpDialog) {
            var otpCode by remember { mutableStateOf("") }
            var otpError by remember { mutableStateOf("") }

            val isResending = remember { mutableStateOf(false) }
            val isVerifying = remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { /* Keep dialog open until success or user closes */ },

                // Rounded corners + custom background color
                shape = RoundedCornerShape(20.dp),
                containerColor = CreamBackground, // Light cream background
                tonalElevation = 8.dp,

                // Icon tinted to match the palette (e.g., Sage Green)
                icon = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_dialog_email),
                        contentDescription = null,
                        tint = SageGreen
                    )
                },
                title = {
                    Text(
                        "Verify Your Email",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = EarthyBrown
                    )
                },
                text = {
                    Column {
                        Text(
                            "We've sent a 6-digit code to $registeredEmail.",
                            color = EarthyBrown
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please enter it below. The code expires after 15 minutes.",
                            color = EarthyBrown
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            singleLine = true,
                            label = { Text("Verification Code", color = EarthyBrown) },
                            textStyle = LocalTextStyle.current.copy(color = EarthyBrown),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = SageGreen,
                                unfocusedBorderColor = Color.LightGray,
                                cursorColor = SageGreen
                            )
                        )

                        if (otpError.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                otpError,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // ---- Resend Button ----
                            Button(
                                onClick = {
                                    isResending.value = true
                                    authService.resendOtp(registeredEmail)
                                        .enqueue(object : Callback<Void> {
                                            override fun onResponse(
                                                call: Call<Void>,
                                                response: Response<Void>
                                            ) {
                                                isResending.value = false
                                                if (response.isSuccessful) {
                                                    Toast.makeText(
                                                        context,
                                                        "Code re-sent to $registeredEmail!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Resend failed: ${response.message()}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }

                                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                                isResending.value = false
                                                Toast.makeText(
                                                    context,
                                                    "Resend error: ${t.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        })
                                },
                                enabled = !isResending.value,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarmOrange // Warm orange for the button
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    if (isResending.value) "Resending..." else "Resend Code",
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // ---- Verify Button ----
                            Button(
                                onClick = {
                                    isVerifying.value = true
                                    otpError = ""

                                    val request = OtpRequest(email = registeredEmail, otpCode = otpCode)
                                    authService.verifyOtp(request).enqueue(object : Callback<String> {
                                        override fun onResponse(call: Call<String>, response: Response<String>) {
                                            isVerifying.value = false
                                            if (response.isSuccessful) {
                                                Toast.makeText(context, "Email Verified!", Toast.LENGTH_SHORT).show()
                                                showOtpDialog = false
                                                navController.navigate("login") {
                                                    popUpTo("register") { inclusive = true }
                                                }
                                            } else {
                                                otpError = response.errorBody()?.string().orEmpty()
                                            }
                                        }

                                        override fun onFailure(call: Call<String>, t: Throwable) {
                                            isVerifying.value = false
                                            otpError = "Network error: ${t.message}"
                                        }
                                    })
                                },
                                enabled = !isVerifying.value,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SageGreen // Earthy green for the verify button
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    if (isVerifying.value) "Verifying..." else "Verify",
                                    color = Color.White
                                )
                            }
                        }
                    }
                },

                confirmButton = {
                    TextButton(onClick = { showOtpDialog = false }) {
                        Text("Close", color = EarthyBrown)
                    }
                }
            )
        }
    }
}
