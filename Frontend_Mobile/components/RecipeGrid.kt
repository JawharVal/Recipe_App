package com.example.recipeapp.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeapp.Recipe

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeGrid(
    navController: NavController,
    recipes: List<Recipe>,
    onDeleteRecipe: (Recipe) -> Unit,
    isMultiSelectMode: Boolean,
    selectedRecipes: List<Recipe>,
    onSelectRecipe: (Recipe) -> Unit,
    onMultiSelectModeToggle: () -> Unit,
    deleteSelectedRecipes: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(recipes) { recipe ->
            RecipeCard(
                recipe = recipe,
                navController = navController,
                onDeleteRecipe = onDeleteRecipe,
                isMultiSelectMode = isMultiSelectMode,
                isSelected = selectedRecipes.contains(recipe),
                onSelectRecipe = onSelectRecipe,
                onMultiSelectModeToggle = onMultiSelectModeToggle,
                selectedCount = selectedRecipes.size,
                deleteSelectedRecipes = { deleteSelectedRecipes() }
            )
        }
    }
}
