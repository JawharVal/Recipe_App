package com.example.recipeapp.screens

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.utils.ApiClient

import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.Book

import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController, context: Context, recipes: MutableList<Recipe>) {
    Log.e("FavoritesScreen", "FavoritesScreen composable is being rendered")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Ensures the screen never shows white
    ) {
    var favoriteRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initialize RecipeRepository
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
    // Fetch favorite recipes and populate author names
    LaunchedEffect(Unit) {
        Log.e("FavoritesScreen", "LaunchedEffect started")
        if (AuthPreferences.isLoggedIn(context)) {
            Log.e("FavoritesScreen", "User is logged in, fetching favorite recipes")
            recipeRepository.getFavoriteRecipes { favorites ->
                if (favorites != null) {
                    Log.e("FavoritesScreen", "Fetched favorites: ${favorites.size}")
                    if (favorites.isEmpty()) {
                        Log.e("FavoritesScreen", "No favorite recipes found")
                        favoriteRecipes = emptyList()
                        isLoading = false
                        return@getFavoriteRecipes
                    }
                    // Initialize a temporary list to hold updated recipes
                    val updatedFavorites = mutableListOf<Recipe>()
                    var processedCount = 0

                    favorites.forEach { recipeDTO ->
                        Log.e("FavoritesScreen", "Processing recipe: ${recipeDTO.title}, authorId: ${recipeDTO.authorId}")
                        val userId = recipeDTO.authorId
                        if (userId != null) {
                            Log.e("FavoritesScreen", "Fetching user for authorId $userId")
                            recipeRepository.getUserById(userId) { user ->
                                val authorName = user?.username ?: "Unknown"
                                Log.e("FavoritesScreen", "Fetched author for userId $userId: $authorName")
                                val updatedRecipe = Recipe(
                                    id = recipeDTO.id,
                                    title = recipeDTO.title,
                                    author = authorName,
                                    authorId = userId,
                                    imageUri = recipeDTO.imageUri,
                                    // Populate other fields as necessary
                                )
                                updatedFavorites.add(updatedRecipe)
                                processedCount++

                                // Once all recipes have been processed, update the state
                                if (processedCount == favorites.size) {
                                    Log.e("FavoritesScreen", "All authors fetched, updating favoriteRecipes")
                                    favoriteRecipes = updatedFavorites
                                    isLoading = false
                                }
                            }
                        } else {
                            Log.e("FavoritesScreen", "Recipe has null authorId, setting author to Unknown")
                            val updatedRecipe = Recipe(
                                id = recipeDTO.id,
                                title = recipeDTO.title,
                                author = "Unknown",
                                authorId = null,
                                imageUri = recipeDTO.imageUri,
                                // Populate other fields as necessary
                            )
                            updatedFavorites.add(updatedRecipe)
                            processedCount++

                            // Check if all recipes have been processed
                            if (processedCount == favorites.size) {
                                Log.e("FavoritesScreen", "All authors fetched (including Unknown), updating favoriteRecipes")
                                favoriteRecipes = updatedFavorites
                                isLoading = false
                            }
                        }
                    }
                } else {
                    Log.e("FavoritesScreen", "Failed to fetch favorite recipes.")
                    errorMessage = "Failed to load favorite recipes."
                    isLoading = false
                }
            }
        } else {
            Log.e("FavoritesScreen", "User not logged in.")
            errorMessage = "User not logged in."
            isLoading = false
        }
    }

    // Handle back navigation
    BackHandler {
        navController.popBackStack()
    }

    var showAddOptionsModal by remember { mutableStateOf(false) }
    var isSheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }


    // Add options modal
    if (showAddOptionsModal) {
        AddOptionsModal(
            navController = navController,
            onDismiss = { showAddOptionsModal = false },
            onAddManually = { isSheetVisible = true }
        )
    }

    // Add recipe bottom sheet
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

    if (isLoading) {
        Log.e("FavoritesScreen", "Displaying loading indicator")
        // Loading Indicator
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (errorMessage != null) {
        Log.e("FavoritesScreen", "Displaying error message: $errorMessage")
        // Error Page
        ErrorPages(
            message = errorMessage!!,
            onRetry = {
                Log.e("FavoritesScreen", "Retrying to fetch favorite recipes")
                isLoading = true
                errorMessage = null
                // Retry fetching favorites
                recipeRepository.getFavoriteRecipes { favorites ->
                    if (favorites != null) {
                        Log.e("FavoritesScreen", "Fetched favorites on retry: ${favorites.size}")
                        if (favorites.isEmpty()) {
                            Log.e("FavoritesScreen", "No favorite recipes found on retry")
                            favoriteRecipes = emptyList()
                            isLoading = false
                            return@getFavoriteRecipes
                        }
                        // Initialize a temporary list to hold updated recipes
                        val updatedFavorites = mutableListOf<Recipe>()
                        var processedCount = 0

                        favorites.forEach { recipeDTO ->
                            Log.e("FavoritesScreen", "Processing recipe on retry: ${recipeDTO.title}, authorId: ${recipeDTO.authorId}")
                            val userId = recipeDTO.authorId
                            if (userId != null) {
                                Log.e("FavoritesScreen", "Fetching user for authorId $userId on retry")
                                recipeRepository.getUserById(userId) { user ->
                                    val authorName = user?.username ?: "Unknown"
                                    Log.e("FavoritesScreen", "Fetched author for userId $userId on retry: $authorName")
                                    val updatedRecipe = Recipe(
                                        id = recipeDTO.id,
                                        title = recipeDTO.title,
                                        author = authorName,
                                        authorId = userId,
                                        imageUri = recipeDTO.imageUri,
                                        // Populate other fields as necessary
                                    )
                                    updatedFavorites.add(updatedRecipe)
                                    processedCount++

                                    // Once all recipes have been processed, update the state
                                    if (processedCount == favorites.size) {
                                        Log.e("FavoritesScreen", "All authors fetched on retry, updating favoriteRecipes")
                                        favoriteRecipes = updatedFavorites
                                        isLoading = false
                                    }
                                }
                            } else {
                                Log.e("FavoritesScreen", "Recipe has null authorId on retry, setting author to Unknown")
                                val updatedRecipe = Recipe(
                                    id = recipeDTO.id,
                                    title = recipeDTO.title,
                                    author = "Unknown",
                                    authorId = null,
                                    imageUri = recipeDTO.imageUri,
                                    // Populate other fields as necessary
                                )
                                updatedFavorites.add(updatedRecipe)
                                processedCount++

                                // Check if all recipes have been processed
                                if (processedCount == favorites.size) {
                                    Log.e("FavoritesScreen", "All authors fetched (including Unknown) on retry, updating favoriteRecipes")
                                    favoriteRecipes = updatedFavorites
                                    isLoading = false
                                }
                            }
                        }
                    } else {
                        Log.e("FavoritesScreen", "Failed to fetch favorite recipes on retry.")
                        errorMessage = "Failed to load favorite recipes."
                        isLoading = false
                    }
                }
            },
            onLogout = {
                Log.e("FavoritesScreen", "Logging out user")
                AuthPreferences.clearToken(context)
                navController.navigate("login") {
                    popUpTo("favorites") { inclusive = true }
                }
            }
        )
    } else {
        Log.e(
            "FavoritesScreen",
            "Displaying main Favorites UI with ${favoriteRecipes.size} recipes"
        )
        // Main Favorites UI
        Scaffold(
            topBar = {
                CookbookDetailTopBar(navController = navController, title = "LeGourmand")
            },
            bottomBar = {
                BottomNavigationBar(
                    navController = navController,
                    selectedTab = "profile",
                    onTabSelected = { /* no-op */ },
                    onAddClick = { showAddOptionsModal = true }
                )
            }
        ) { paddingValues ->
            if (favoriteRecipes.isEmpty()) {
                Log.e("FavoritesScreen", "No favorite recipes to display")
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You have no favorite recipes yet.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Log.e("FavoritesScreen", "Displaying ${favoriteRecipes.size} favorite recipes")
                // List of Favorite Recipes
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    items(favoriteRecipes) { recipe ->
                        FavoriteRecipeItem(recipe = recipe, onClick = {
                            navController.navigate("recipeDetail/${recipe.id}")
                        })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
    }
}

@Composable
fun FavoriteRecipeItem(recipe: Recipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color.White, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE4353535)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Recipe Image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .border(width = 2.dp, color = Color.White, shape = RoundedCornerShape(1.dp))
                    .background(Color.Gray, shape = RoundedCornerShape(1.dp))
            ) {
                if (!recipe.imageUri.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = recipe.imageUri),
                        contentDescription = recipe.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Placeholder Icon",
                        tint = Color.LightGray,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Recipe Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = recipe.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Author: ${recipe.author}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ErrorPages(
    message: String,
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onLogout) {
                Text("Logout")
            }
        }
    }
}
