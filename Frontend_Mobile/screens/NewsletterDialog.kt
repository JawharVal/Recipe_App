// app/src/main/java/com/example/recipeapp/screens/NewsletterDialog.kt
package com.example.recipeapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsletterDialog(
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit,
    onError: (String) -> Unit
) {
    var isSubscribing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubscribing) onDismiss() },
        title = {
            Text(
                text = "Subscribe to Newsletter",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Stay updated with the latest recipes and updates from LeGourmand.",
                color = Color.Gray
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubscribing = true
                    onSubscribe()
                },
                enabled = !isSubscribing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF876232),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF444444),
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSubscribing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Subscribe", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isSubscribing) onDismiss() },
                enabled = !isSubscribing,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.Gray
                )
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1F1F1F)
    )
}
