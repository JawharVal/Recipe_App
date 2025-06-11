package com.example.recipeapp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SectionCard(
    backgroundColor: Color,
    borderColor: Color,
    borderWidth: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    trailingText: String? = null,
    shouldShowError: Boolean,
    isNumeric: Boolean = false,
    isUrl: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = Int.MAX_VALUE
) {
    // Define a variable to hold a dynamically updated error message
    var dynamicErrorMessage by remember { mutableStateOf<String?>(null) }

    // Define a function to validate URL format
    fun isValidUrl(url: String): Boolean {
        return url.isEmpty() || android.util.Patterns.WEB_URL.matcher(url).matches()
    }

    // Handle URL validation if `isUrl` is true
    val showUrlError = isUrl && !isValidUrl(value)
    val displayErrorMessage = if (showUrlError) "Please enter a valid URL" else errorMessage

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                if (isUrl && !isValidUrl(it)) {
                    dynamicErrorMessage = "Please enter a valid URL"
                } else {
                    dynamicErrorMessage = null
                }
            },
            label = { Text(label, color = Color.Gray) },
            trailingIcon = { trailingText?.let { Text(it, color = Color.Gray) } },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = Color.White),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Black
            ),
            singleLine = singleLine,
            maxLines = maxLines
        )

        // Display error message if there is an error
        if (shouldShowError && (dynamicErrorMessage != null || displayErrorMessage != null)) {
            Text(
                text = dynamicErrorMessage ?: displayErrorMessage!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}



@Composable
fun TagChip(tag: String, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color.Gray, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tag, color = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove tag", tint = Color.White)
            }
        }
    }
}

@Composable
fun TagDialog(
    tagSearch: String,
    onTagSearchChange: (String) -> Unit,
    onTagAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create tags") },
        text = {
            Column {
                OutlinedTextField(
                    value = tagSearch,
                    onValueChange = onTagSearchChange,
                    label = { Text("Create tags...") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                        keyboardType = KeyboardType.Text // Adjusts the type of keyboard
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Create new tag option
                OutlinedButton(
                    onClick = { onTagAdd(tagSearch) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Tag")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create tag")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutrientField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    shouldShowError: Boolean
) {
    var showError by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
            showError = newValue.isNotEmpty() && newValue.toIntOrNull() == null // Show error if non-numeric
        },
        label = { Text(label, color = if (showError) Color.Red else Color.Gray) },
        trailingIcon = { Text(unit, color = Color.Gray) },
        isError = showError && shouldShowError, // Error only if shouldShowError is true
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done, // Changes keyboard button to "Done"
            keyboardType = KeyboardType.Text // Adjusts the type of keyboard
        ),
        textStyle = TextStyle(color = Color.White)
    )

    // Show error message if necessary
    if (showError && shouldShowError) {
        Text(
            text = "Please enter a valid number",
            color = Color.Red,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomOutlinedDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String> = emptyList(), // Pass options for dropdown
    trailingText: String? = null,
    shouldShowError: Boolean = false,
    errorMessage: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label, color = Color.Gray) },
                trailingIcon = {
                    if (options.isNotEmpty()) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    } else if (trailingText != null) {
                        Text(trailingText, color = Color.Gray)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .clickable(enabled = options.isNotEmpty()) { expanded = true },
                readOnly = options.isNotEmpty(),
                textStyle = TextStyle(color = Color.White),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color.Black
                )
            )

            if (options.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        if (shouldShowError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun DeleteRecipeButtonWithConfirmation(navController: NavController) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDialog = true },
        border = BorderStroke(1.dp, Color.Red),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
        modifier = Modifier
            .padding(start = 16.dp, bottom = 16.dp)
            .fillMaxWidth(0.45f)
    ) {
        Icon(Icons.Default.Delete, contentDescription = "Delete recipe", tint = Color.Red)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Delete")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete this recipe?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        navController.popBackStack()  // Simulates going back to the home screen
                    }
                ) {
                    Text("Yes", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}


