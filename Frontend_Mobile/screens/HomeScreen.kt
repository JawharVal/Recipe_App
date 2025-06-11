package com.example.recipeapp.screens

import SearchBar
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal

import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.components.RecipeGrid
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.ApiClient.recipeService
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.BookRepository
import com.example.recipeapp.utils.ErrorPage

import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO

import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF876232))
    }
}

@Composable
fun TopBar(navController: NavController) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp, vertical = 8.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Empty Spacer to balance the start of the Row
            Spacer(modifier = Modifier.weight(1f))

            // Logo and Title centered
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(16f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_loogo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(48.dp), // Adjust size as needed
                    tint = Color(0xFF886232)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "LeGourmand", fontSize = 20.sp, color = Color.White)
            }

            // Settings Icon aligned to the right
            Text(
                text = "Upgrade",
                modifier = Modifier
                    .clickable { navController.navigate("premium") }
                    .padding(0.5.dp),
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Cursive,
                style = MaterialTheme.typography.bodyMedium
            )

        }

        // Separator line below the top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Gray.copy(alpha = 0.3f)) // Light gray line
        )
    }
}
@Composable
fun TabButtons(
    navController: NavController,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color(0xFF2E2E2E), shape = MaterialTheme.shapes.medium),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // "My Recipes" Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    if (selectedTab == "home") Color(0xFF1F1F1F) else Color(0xFF2E2E2E),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(vertical = 12.dp)
                .clickable {
                    onTabSelected("home")
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_recipes),
                    contentDescription = "My Recipes Icon",
                    tint = if (selectedTab == "home") Color(0xFF876232) else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = "My Recipes",
                    color = if (selectedTab == "home") Color.White else Color.Gray,
                    fontSize = 16.sp
                )
            }
        }

        // "Discover" Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    if (selectedTab == "discover") Color(0xFF1F1F1F) else Color(0xFF2E2E2E),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(vertical = 12.dp)
                .clickable {
                    onTabSelected("discover")
                    navController.navigate("discover") {
                        popUpTo("home") { inclusive = true }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_discover),
                    contentDescription = "Discover Icon",
                    tint = if (selectedTab == "discover") Color(0xFF876232) else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = "Discover",
                    color = if (selectedTab == "discover") Color.White else Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
    }
}
//
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, onAddClick: () -> Unit, recipes: MutableList<Recipe>) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetVisible by remember { mutableStateOf(false) }
    val filteredRecipes = remember { mutableStateListOf<Recipe>().apply { addAll(recipes) } }
    var selectedTab by remember { mutableStateOf("home") }
    // Multi-select mode state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedRecipes = remember { mutableStateListOf<Recipe>() }
    var showAddOptionsModal by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Context for token retrieval
    val context = LocalContext.current
    val bookRepository = remember { BookRepository(context) }
    // Fetch recipes when the HomeScreen is first composed
    val recipeRepository = remember { RecipeRepository(context) }

    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }

    val authService = ApiClient.getAuthService(context)
    LaunchedEffect(Unit) {
        authService.getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        when (user.subscriptionType?.uppercase() ?: "FREE") {
                            "FREE" -> {
                                plusUnlocked = false
                                proUnlocked = false
                            }
                            "PLUS" -> {
                                plusUnlocked = true
                                proUnlocked = false
                            }
                            "PRO" -> {
                                plusUnlocked = true
                                proUnlocked = true
                            }
                        }
                    }
                } else {
                    Log.e("CookbooksOverviewScreen", "Failed to fetch profile: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("CookbooksOverviewScreen", "Error fetching profile: ${t.localizedMessage ?: "Unknown error"}")
            }
        })
    }
    LaunchedEffect(Unit) {
        val token = AuthPreferences.getToken(context)
        if (token != null) {
            recipeRepository.getUserRecipes(token) { userRecipes ->
                if (userRecipes != null) {
                    recipes.clear()
                    recipes.addAll(userRecipes)
                    filteredRecipes.clear()
                    filteredRecipes.addAll(userRecipes)
                    isLoading = false // Stop loading spinner
                } else {
                    errorMessage = "Failed to fetch your recipes."
                    isLoading = false
                }
            }
        } else {
            errorMessage = "User not logged in."
            isLoading = false
        }
    }

    // Function to handle recipe selection
    fun onSelectRecipe(recipe: Recipe) {
        if (selectedRecipes.contains(recipe)) {
            selectedRecipes.remove(recipe)
        } else {
            selectedRecipes.add(recipe)
        }
    }

    // Function to toggle multi-select mode
    fun onMultiSelectModeToggle() {
        isMultiSelectMode = !isMultiSelectMode
        if (!isMultiSelectMode) {
            selectedRecipes.clear() // Clear selections when exiting multi-select mode
        }
    }

    // Function to delete selected recipes
    fun deleteSelectedRecipes() {
        val token = AuthPreferences.getToken(context)
        if (token != null) {
            val recipeIds = selectedRecipes.mapNotNull { it.id }
            if (recipeIds.isNotEmpty()) {
                recipeRepository.deleteSelectedRecipes(token, recipeIds) { success ->
                    if (success) {
                        recipes.removeAll { it.id in recipeIds }
                        filteredRecipes.removeAll { it.id in recipeIds }
                        selectedRecipes.clear()
                        isMultiSelectMode = false
                    } else {
                        errorMessage = "Failed to delete selected recipes."
                    }
                }
            } else {
                // Optionally, inform the user that no valid recipes are selected
            }
        } else {
            errorMessage = "User not logged in."
        }
    }

    // Function to delete a single recipe
    fun onDeleteRecipe(recipe: Recipe) {
        val token = AuthPreferences.getToken(context)
        if (token != null) {
            recipe.id?.let { recipeId ->
                // 1) Remove this recipe from all books
                bookRepository.removeRecipeFromAllBooks(recipeId) { removalSuccess ->
                    if (!removalSuccess) {
                        // Could not remove recipe from books; stop here
                        errorMessage = "Failed to remove recipe from cookbooks. Cannot delete."
                        return@removeRecipeFromAllBooks
                    }

                    // 2) If removal was successful, delete the recipe itself
                    recipeRepository.deleteRecipe(recipeId, token) { deleteSuccess ->
                        if (deleteSuccess) {
                            recipes.remove(recipe)
                            filteredRecipes.remove(recipe)
                            Toast.makeText(context, "Recipe deleted successfully.", Toast.LENGTH_SHORT).show()
                        } else {
                            errorMessage = "Failed to delete the recipe."
                        }
                    }
                }
            } ?: run {
                // Recipe ID is null
                errorMessage = "Recipe ID is null. Cannot delete."
            }
        } else {
            errorMessage = "User not logged in. Cannot delete recipe."
        }
    }

    // Handle Add Options Modal and Bottom Sheet
    if (showAddOptionsModal) {
        AddOptionsModal(
            navController = navController,
            onDismiss = { showAddOptionsModal = false },
            onAddManually = { isSheetVisible = true }
        )
    }
    if (isSheetVisible) {
        val currentSubscriptionType = when {
            proUnlocked -> "PRO"
            plusUnlocked -> "PLUS"
            else -> "FREE"
        }
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                    isSheetVisible = false
                }
            },
            sheetState = sheetState
        ) {
            AddRecipeBottomSheetContent(
                navController = navController,
                onClose = {
                    isSheetVisible = false
                    coroutineScope.launch { sheetState.hide() }
                },
                recipes = recipes,
                subscriptionType = currentSubscriptionType
            )
        }
    }



    when { errorMessage != null -> {
        // Show ErrorPage when there's an error
        ErrorPage(
            message = errorMessage ?: "An unexpected error occurred.",
            navController = navController,
            onRetry = {
                isLoading = true
                errorMessage = null
                val token = AuthPreferences.getToken(context)
                if (token != null) {
                    recipeRepository.getUserRecipes(token) { userRecipes ->
                        if (userRecipes != null) {
                            recipes.clear()
                            recipes.addAll(userRecipes)
                            filteredRecipes.clear()
                            filteredRecipes.addAll(userRecipes)
                            isLoading = false
                        } else {
                            errorMessage = "Failed to fetch your recipes."
                            isLoading = false
                        }
                    }
                } else {
                    errorMessage = "User not logged in."
                    isLoading = false
                }
            },
            onLogout = {
                AuthPreferences.clearToken(context)
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
        )

    }
        else -> {  // Main Home UI
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        navController = navController,
                        selectedTab = selectedTab, // Pass selectedTab
                        onTabSelected = { selectedTab = it }, // Update selected tab on click
                        onAddClick = { showAddOptionsModal = true } // Show Add Options Modal
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background image filling the entire screen
                    Image(
                        painter = painterResource(id = R.drawable.ds),
                        contentDescription = "Background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            // Add a blur effect, adjust the dp to control intensity
                            .blur(4.dp)
                    )
                    // Overlay your content on top of the background image
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        TopBar(navController)
                        // Conditionally render based on loading and error states

                                Spacer(modifier = Modifier.height(16.dp))
                                TabButtons(
                                    navController = navController,
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                SearchBar(
                                    recipes = recipes,
                                    onSearchResult = { filtered ->
                                        filteredRecipes.clear()
                                        filteredRecipes.addAll(filtered)
                                    },
                                    availableTags = listOf(
                                        "Vegan",
                                        "Vegetarian",
                                        "Gluten-Free",
                                        "Dairy-Free",
                                        "Keto",
                                        "Paleo",
                                        "Low Carb",
                                        "High Protein",
                                        "Quick Meals",
                                        "Easy",
                                        "Healthy",
                                        "Comfort Food",
                                        "Spicy",
                                        "Sweet",
                                        "Savory",
                                        "Breakfast",
                                        "Lunch",
                                        "Dinner",
                                        "Snack",
                                        "Holiday"
                                    ), // Example tags
                                    availableCuisines = listOf(
                                        "Russian",
                                        "Italian",
                                        "Mexican",
                                        "Indian",
                                        "Chinese",
                                        "Japanese",
                                        "American",
                                        "French",
                                        "Arabian",
                                        "Thai",
                                        "Spanish",
                                        "Mediterranean",
                                        "Korean",
                                        "Vietnamese"
                                    ),
                                    availableDifficulties = listOf(
                                        "Easy",
                                        "Medium",
                                        "Hard"
                                    ) // Example difficulties
                                )
                                Spacer(modifier = Modifier.height(22.dp))
                        AnimatedBorderBox(navController)
                        Spacer(modifier = Modifier.height(8.dp))




                        when {
                            isLoading -> {
                                LoadingIndicator()
                            }

                            else -> {
                                when {
                                    recipes.isEmpty() -> {
                                        AddRecipeSection(
                                            navController = navController,
                                            recipes = recipes
                                        )
                                    }

                                    filteredRecipes.isEmpty() -> {
                                        Text(
                                            text = "No recipes found",
                                            color = Color.White,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }

                                    else -> {
                                        RecipeGrid(
                                            navController = navController,
                                            recipes = filteredRecipes,
                                            onDeleteRecipe = ::onDeleteRecipe,
                                            isMultiSelectMode = isMultiSelectMode,
                                            selectedRecipes = selectedRecipes,
                                            onSelectRecipe = ::onSelectRecipe,
                                            onMultiSelectModeToggle = ::onMultiSelectModeToggle,
                                            deleteSelectedRecipes = ::deleteSelectedRecipes
                                        )
                                    }
                                }


                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AnimatedBorderBox(navController: NavController) {
    val transition = rememberInfiniteTransition()

    // Animate color positions
    val gradientOffset by transition.animateFloat(
        initialValue = -510f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Animated gradient brush
    val animatedBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF332E2A),Color(0xFF876232),Color(0xFF876232), Color(0xFF876232), Color(0xFF332E2A),), // Adjust colors
        startX = gradientOffset,
        endX = gradientOffset + 600f
    )

    // Border container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF181616))
            .border(BorderStroke(4.dp, animatedBrush), RoundedCornerShape(28.dp)) // ✅ Animated border applied
            .clickable { navController.navigate("challenges") }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uD83D\uDD25 Join Cooking Challenges! \uD83D\uDD25",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}