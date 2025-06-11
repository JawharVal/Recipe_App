package com.example.recipeapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.recipeapp.Recipe
import com.example.recipeapp.R
import coil.compose.rememberAsyncImagePainter
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipesDialog(
    bookId: Long,
    onDismiss: () -> Unit,
    onAddRecipes: (List<Long>) -> Unit,
    allRecipes: List<Recipe>,
    favoriteRecipes: List<Recipe>,
    currentUserId: Long?
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRecipes by remember { mutableStateOf(setOf<Long>()) }
    var showOwnRecipes by remember { mutableStateOf(true) } // Toggle between own/favorites

    // Filter recipes based on toggle
    val filteredRecipes = (if (showOwnRecipes) {
        allRecipes.filter { it.authorId == currentUserId }
    } else {
        favoriteRecipes.filter { it.authorId != currentUserId }
    }).filter { it.title.contains(searchQuery, ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2E2E2E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Recipes",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_loogo),
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Buttons for Recipe Filtering
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterButton(
                        text = "Own Recipes",
                        isSelected = showOwnRecipes,
                        onClick = { showOwnRecipes = true }, // Toggle state here
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterButton(
                        text = "Favorites",
                        isSelected = !showOwnRecipes,
                        onClick = { showOwnRecipes = false }, // Toggle state here
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(text = "Search by title", color = Color.Gray) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Search Icon",
                            tint = Color.Gray
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color(0xFF886332),
                        focusedBorderColor = Color(0xFF886332),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Recipes Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .height(300.dp)
                        .fillMaxWidth()
                ) {
                    items(filteredRecipes) { recipe ->
                        SelectableRecipeCard(
                            recipe = recipe,
                            isSelected = selectedRecipes.contains(recipe.id),
                            onSelect = {
                                val recipeId = recipe.id
                                if (recipeId != null) {
                                    selectedRecipes = if (selectedRecipes.contains(recipeId)) {
                                        selectedRecipes - recipeId
                                    } else {
                                        selectedRecipes + recipeId
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Add Button
                Button(
                    onClick = { onAddRecipes(selectedRecipes.toList()) },
                    enabled = selectedRecipes.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF886232)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Add to cookbook", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SelectableRecipeCard(recipe: Recipe, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Increased height for better layout
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                // Recipe Image or Placeholder
                if (recipe.imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = recipe.imageUri),
                        contentDescription = "Recipe Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF886332), shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Chef Hat",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                // Checkbox
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Select Recipe",
                    tint = if (isSelected) Color(0xFF886332) else Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
            // Recipe Title
            Text(
                text = recipe.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun FilterToggleButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterButton(
            text = "Own Recipes",
            isSelected = true,
            onClick = { /* handle click */ },
            modifier = Modifier.weight(1f) // using weight
        )
        FilterButton(
            text = "Favorites",
            isSelected = false,
            onClick = { /* handle click */ },
            modifier = Modifier.weight(1f) // using weight
        )
    }
}

@Composable
fun FilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier, // pass the modifier here
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF896232) else Color.DarkGray
        )
    ) {
        Text(text, color = Color.White)
    }
}

