package com.example.recipeapp.screens

import android.content.Context
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recipeapp.R
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthService
import com.example.recipeapp.utils.ForgotPasswordRequest
import com.example.recipeapp.utils.UpdatePasswordRequest
import com.example.recipeapp.utils.UserDTO
import com.example.recipeapp.utils.VerifyResetCodeRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


@Composable
fun produceTimeRemaining(deadline: Long): State<Int> {
    // This will emit the number of seconds left until `deadline` is reached
    // or zero if it's already expired.
    return produceState(initialValue = 0, deadline) {
        while (true) {
            val now = System.currentTimeMillis()
            val millisLeft = deadline - now
            if (millisLeft <= 0) {
                value = 0
                break
            } else {
                value = (millisLeft / 1000).toInt()
            }
            kotlinx.coroutines.delay(1000)
        }
    }
}

@Composable
fun CooldownButton(
    isEnabled: Boolean,
    timeRemaining: Int,
    onClick: () -> Unit,
    enabledGradient: Brush,
    disabledGradient: Brush,
    textEnabled: String
) {
    val backgroundBrush = if (isEnabled) enabledGradient else disabledGradient

    Button(
        onClick = { if (isEnabled) onClick() },
        enabled = isEnabled,
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
                    brush = backgroundBrush,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isEnabled) {
                Text(textEnabled, fontSize = 18.sp, color = Color.White)
            } else {
                Text("Resend in ${timeRemaining}s", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    val context = LocalContext.current
    val authService = ApiClient.getRetrofit(context).create(AuthService::class.java)

    // This SharedPreferences is scoped to your app.
    // You can replace "my_prefs" with any name you like.
    val prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

    // -------------- State --------------
    // We no longer rely purely on rememberSaveable for 'deadline'
    var deadline by remember { mutableStateOf(0L) }

    // We'll track the current step in your reset process:
    var step by rememberSaveable { mutableStateOf(1) }

    // Fields for each step
    var email by rememberSaveable { mutableStateOf("") }
    var resetCode by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    // -------------- Read the saved deadline --------------
    // Use a LaunchedEffect so this runs once on entering the screen.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val storedDeadline = prefs.getLong("forgotPassDeadline", 0L)
        // If storedDeadline is still in the future, restore it
        if (storedDeadline > System.currentTimeMillis()) {
            deadline = storedDeadline
        } else {
            // Otherwise, ensure we reset it
            deadline = 0L
        }
    }
    val passwordRegex = Regex("^(?=.*[A-Z])(?=.*\\d).{8,}$")
    // We'll compute how many seconds remain by comparing "deadline" to System.currentTimeMillis()
    val timeRemaining by produceTimeRemaining(deadline)
    val isButtonEnabled = timeRemaining <= 0

    // Main UI
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Forgot Password", fontSize = 24.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))

            when (step) {
                1 -> {
                    // STEP 1: ENTER EMAIL
                    Text("Enter your email address to reset password", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                            keyboardType = KeyboardType.Text // Adjusts the type of keyboard
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // The cooldown button
                    CooldownButton(
                        isEnabled = isButtonEnabled,
                        timeRemaining = timeRemaining,
                        onClick = {
                            // 1) Email format validation
                            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                Toast.makeText(context, "Unrecognized email address", Toast.LENGTH_SHORT).show()
                                return@CooldownButton
                            }
                            // 1) Set a new deadline
                            val newDeadline = System.currentTimeMillis() + 60_000 // 1 minute
                            deadline = newDeadline

                            // 2) Save to SharedPreferences
                            prefs.edit()
                                .putLong("forgotPassDeadline", newDeadline)
                                .apply()

                            // 3) Call forgotPassword
                            val request = ForgotPasswordRequest(email)
                            authService.forgotPassword(request).enqueue(object : Callback<String> {
                                override fun onResponse(call: Call<String>, response: Response<String>) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "If the email exists, a code was sent.", Toast.LENGTH_SHORT).show()
                                        step = 2 // move to step 2
                                    } else {
                                        Toast.makeText(context, "Unrecognized Email", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<String>, t: Throwable) {
                                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        },
                        enabledGradient = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4C74AF), Color(0xFF8881C7))
                        ),
                        disabledGradient = Brush.horizontalGradient(
                            colors = listOf(Color.LightGray, Color.Gray)
                        ),
                        textEnabled = "Send Verification Code"
                    )
                }

                2 -> {
                    // STEP 2: ENTER CODE
                    Text("Enter the 6-digit code sent to your email", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetCode,
                        onValueChange = { resetCode = it },
                        label = { Text("Reset Code") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val request = VerifyResetCodeRequest(email, resetCode)
                            authService.verifyResetCode(request).enqueue(object : Callback<String> {
                                override fun onResponse(call: Call<String>, response: Response<String>) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Code verified!", Toast.LENGTH_SHORT).show()
                                        step = 3
                                    } else {
                                        Toast.makeText(context, "Incorrect verification code", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<String>, t: Throwable) {
                                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify Code")
                    }
                }

                3 -> {
                    // STEP 3: ENTER NEW PASSWORD
                    Text("Enter your new password", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            // 1) Check if newPassword meets your password criteria
                            if (!passwordRegex.matches(newPassword)) {
                                Toast.makeText(
                                    context,
                                    "Password must be at least 8 characters, 1 uppercase letter, and 1 digit.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            // 2) Check if passwords match
                            if (newPassword != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // If the checks pass, proceed with the API call
                            val request = UpdatePasswordRequest(email, newPassword)
                            authService.updatePasswordAfterVerification(request).enqueue(object : Callback<String> {
                                override fun onResponse(call: Call<String>, response: Response<String>) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                                        // Navigate back to login
                                        navController.navigate("login") {
                                            popUpTo("forgotPassword") { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(context, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<String>, t: Throwable) {
                                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Password")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Back to Login"
            Text(
                "Back to Login",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    navController.navigate("login") {
                        popUpTo("forgotPassword") { inclusive = true }
                    }
                }
            )
        }
    }
}
