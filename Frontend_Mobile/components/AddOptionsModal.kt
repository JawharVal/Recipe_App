package com.example.recipeapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOptionsModal(
    navController: NavController,
    onDismiss: () -> Unit,
    onAddManually: () -> Unit
) {
    // ModalBottomSheet with improved background color and padding
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xE9FAFAFA), // Lighter background for better contrast
        shape = MaterialTheme.shapes.large // Rounded corners for modern look
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Improved spacing between elements
        ) {
            // Title with larger font size and subtle shadow for emphasis
            Text(
                text = "Add Recipe",
                style = MaterialTheme.typography.headlineMedium.copy(color = Color(0xFF212121)),
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color(0xFF212121) // Darker text color for better readability
            )

            // Generate Recipe Button with enhanced styling
            Button(
                onClick = {
                    onDismiss()
                    navController.navigate("generateRecipe")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Fixed height for consistency
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF886332), // Green color for action button
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium // Rounded corners
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing between icon and text
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ai),
                        contentDescription = "Generate Recipe",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp) // Consistent icon size
                    )
                    Text(
                        text = "Generate recipe with AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            // Add Manually Button with enhanced styling
            Button(
                onClick = {
                    onDismiss()
                    onAddManually()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Fixed height for consistency
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2F7958), // Gray color for secondary button
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium // Rounded corners
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing between icon and text
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_edit),
                        contentDescription = "Add Recipe",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp) // Consistent icon size
                    )
                    Text(
                        text = "Add recipe manually",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            // Cancel Button (Optional) - Allows users to dismiss the modal easily
            TextButton(
                onClick = { onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF757575) // Subtle gray color for cancel action
                )
            }
        }
    }
}