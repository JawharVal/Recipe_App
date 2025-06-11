package com.example.recipeapp.components

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeapp.R
import com.example.recipeapp.Recipe

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Add

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.recipeapp.screens.BookSelectionBottomSheet
import com.example.recipeapp.utils.BookRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCard(
    recipe: Recipe,
    navController: NavController,
    onDeleteRecipe: (Recipe) -> Unit,
    isMultiSelectMode: Boolean, // To track multi-select mode
    isSelected: Boolean, // To track individual recipe selection
    onSelectRecipe: (Recipe) -> Unit,
    onMultiSelectModeToggle: () -> Unit,
    selectedCount: Int,
    deleteSelectedRecipes: () -> Unit // Add callback for deleting the recipe
) {

    val context = LocalContext.current
    val bookRepository = remember { BookRepository(context) }
    // Remember states to control visibility of the menu and confirmation dialog
    val showMenu = remember { mutableStateOf(false) }
    val showDeleteConfirmation = remember { mutableStateOf(false) }
    var showBookSelectionSheet by remember { mutableStateOf(false) }
    val onDismiss = { showMenu.value = false }
    val onShowDeleteConfirmation = { showDeleteConfirmation.value = true }
    val onShowBookSelection = { showBookSelectionSheet = true }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isMultiSelectMode) {
                            onSelectRecipe(recipe)
                        } else {
                            navController.navigate("recipeDetail/${recipe.id}")
                        }
                    },
                    onLongPress = { showMenu.value = true }
                )
            }
            .padding(8.dp)
            .width(160.dp)
    ) {
        // Outer box representing the gray rectangle
        Box(
            modifier = Modifier
                .size(190.dp)
                .background(Color(0xFA8B6332), shape = RoundedCornerShape(12.dp))
                .padding(6.dp)
        ) {
            if (recipe.imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = recipe.imageUri),
                    contentDescription = "Recipe Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_placehofflder),
                    contentDescription = "Placeholder Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            // Display checkbox in bottom right corner if multi-select mode is active
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectRecipe(recipe) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF328762), // background when checked
                        checkmarkColor = Color.White      // color of the tick (change this value)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
        }


        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = recipe.title ?: "Untitled",
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = showMenu.value,
            onDismissRequest = onDismiss
        ) {
            if (isMultiSelectMode && selectedCount > 0) {
                // In multi-select mode: show only the delete option
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onShowDeleteConfirmation()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            } else {
                // Normal mode: show multi-select, add to cookbook, then delete.
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Multi select",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onMultiSelectModeToggle()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_input_add),
                            contentDescription = "Delete",
                            tint = Color(0xFF886232)
                        )
                    }
                )
                Divider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Add to cookbook",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onShowBookSelection()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cookbooks),
                            contentDescription = "Delete",
                            tint = Color(0xFF886232)
                        )
                    }
                )
                Divider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onShowDeleteConfirmation()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }

        }
        // Show the bottom sheet if needed
        if (showBookSelectionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBookSelectionSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                // Build the content of the bottom sheet, passing in
                // the recipe, BookRepository, and a callback to close
                BookSelectionBottomSheet(
                    recipe = recipe!!,
                    bookRepository = bookRepository,
                    onClose = { showBookSelectionSheet = false }
                )
            }
        }
// Delete confirmation dialog
        val context = LocalContext.current
        if (showDeleteConfirmation.value) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = "Delete Recipe",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete this recipe?",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isMultiSelectMode) {
                                deleteSelectedRecipes() // Multi-select delete callback
                            } else {
                                onDeleteRecipe(recipe) // Single recipe delete callback
                            }
                            onDismiss()
                            Toast.makeText(
                                context,
                                "Recipe deleted successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteConfirmation.value = false },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }
                }
,
                        containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}
