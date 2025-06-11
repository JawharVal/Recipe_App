package com.example.recipeapp.utils


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShoppingItemSection(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onAddItem: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Outlined Text Field for item input
        OutlinedTextField(
            value = inputText,
            onValueChange = { onInputTextChange(it) },
            label = {
                Text(
                    text = "Add Item",
                    color = Color.Gray // Optional: Set label color for better visibility
                )
            },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            singleLine = true,
            textStyle = TextStyle(color = Color.White), // Set input text color to white
            colors = TextFieldDefaults.outlinedTextFieldColors(

                cursorColor = Color.White, // Sets cursor color to white
                focusedBorderColor = Color(0xFF876232), // Optional: Customize border colors
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF876232),
                unfocusedLabelColor = Color.Gray
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),

        )


        // Floating Action Button with Plus Sign
        FloatingActionButton(
            onClick = { onAddItem() },
            containerColor = Color(0xFF876232),
            contentColor = Color.Black,
            modifier = Modifier.size(56.dp) // Standard FAB size
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Item",
                modifier = Modifier.size(24.dp) // Larger icon for prominence
            )
        }
    }
}
