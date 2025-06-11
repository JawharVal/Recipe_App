// app/src/main/java/com/example/recipeapp/screens/FeedbackDialog.kt
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
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onFeedbackSend: (String) -> Unit
) {
    var feedback by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = {
            Text(
                text = "Send Feedback",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Please let us know what you think about LeGourmand so we can continue to improve the app.",
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Your Feedback", color = Color.White) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 5,
                    singleLine = false,
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color(0xFF876232),
                        focusedBorderColor = Color(0xFF876232),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF876232),
                        unfocusedLabelColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (feedback.isNotBlank()) {
                        isSending = true
                        onFeedbackSend(feedback)
                    }
                },
                enabled = feedback.isNotBlank() && !isSending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF876232),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF444444),
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Submit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isSending) onDismiss() },
                enabled = !isSending
            ) {
                Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1F1F1F)
    )
}