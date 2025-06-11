package com.example.recipeapp.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recipeapp.Recipe

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AddRecipeBottomSheetContent(
    navController: NavController,
    onClose: () -> Unit,
    initialTitle: String = "",
    recipes: MutableList<Recipe>,
    subscriptionType: String // e.g., "FREE", "PLUS", "PRO"
) {
    var title by remember { mutableStateOf(initialTitle) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var shouldShowErrors by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Determine maximum allowed recipes based on the subscription type.
    val maxRecipesAllowed = when (subscriptionType.uppercase()) {
        "FREE" -> 10
        "PLUS" -> 25
        "PRO" -> Int.MAX_VALUE // Unlimited recipes
        else -> 10
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title Header
        Text(
            text = "Add New Recipe",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "What is the title of the recipe you want to add?",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Title Input Field
        OutlinedTextField(
            value = title,
            onValueChange = { newText ->
                title = newText
                if (shouldShowErrors) {
                    titleError = null
                }
            },
            label = { Text("Title", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Black
            ),
            isError = titleError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        )

        // Error Message
        if (titleError != null) {
            Text(
                text = titleError ?: "",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Recipe Button
        Button(
            onClick = {
                // Check if the user has reached the maximum allowed recipes.
                if (recipes.size >= maxRecipesAllowed) {
                    Toast.makeText(
                        context,
                        "You have reached the maximum number of recipes allowed for the $subscriptionType plan.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                val trimmedTitle = title.trim()
                val recipeExists = recipes.any { it.title.equals(trimmedTitle, ignoreCase = true) }

                if (trimmedTitle.isNotEmpty() && !recipeExists) {
                    shouldShowErrors = true
                    val existingRecipeIndex = recipes.indexOfFirst { it.title == initialTitle }

                    if (existingRecipeIndex != -1) {
                        // Update existing recipe
                        recipes[existingRecipeIndex] = recipes[existingRecipeIndex].copy(title = trimmedTitle)
                        onClose()
                    } else {
                        // Navigate to create recipe screen
                        onClose()
                        val encodedTitle = Uri.encode(trimmedTitle)
                        navController.navigate("createRecipe?title=$encodedTitle")
                    }
                } else {
                    titleError = when {
                        trimmedTitle.isEmpty() -> "Title is required."
                        recipeExists -> "A recipe with this title already exists."
                        else -> null
                    }
                }
            },
            enabled = title.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF307A5A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Add New Recipe",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}