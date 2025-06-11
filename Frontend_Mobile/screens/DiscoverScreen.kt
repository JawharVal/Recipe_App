package com.example.recipeapp.screens

import RelativeTimeText
import android.util.Log
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.utils.RecipeRepository
import kotlinx.coroutines.launch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.ChallengeApi
import com.example.recipeapp.utils.FeaturedWinner

import com.example.recipeapp.utils.UserDTO
import com.example.recipeapp.utils.formatRelativeTime
import filterRecipes
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import parseCreatedAt
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    navController: NavController,
    onAddClick: () -> Unit,
    recipes: MutableList<Recipe>
) {

    val context = LocalContext.current
    val recipeRepository = remember { RecipeRepository(context) }
    val allRecipes = remember { mutableStateListOf<Recipe>() }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // State for Search and Filters
    var searchQuery by remember { mutableStateOf("") }
    var topWinners by remember { mutableStateOf<List<FeaturedWinner>>(emptyList()) }
    LaunchedEffect(Unit) {
        try {
            val api = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)
            topWinners = api.getFeaturedWinners() // Assumes winners are sorted (first is top)
        } catch (e: Exception) {
            Log.e("DiscoverScreen", "Error fetching top winners: ${e.localizedMessage}")
        }
    }

    val showFilters = remember { mutableStateOf(false) }
    val selectedTags = remember { mutableStateListOf<String>() }
    val selectedCuisine = remember { mutableStateOf<String?>(null) }
    val selectedDifficulty = remember { mutableStateOf<String?>(null) }

    // Available filter options
    val availableFilters = listOf("Vegan", "Dessert", "Quick Meals", "American")
    var isSheetVisible by remember { mutableStateOf(false) }
    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }

    // State for Filtered Recipes
    // 1. Update your selectedFilters state to include "Tags"
    val selectedFilters = remember {
        mutableStateMapOf(
            "Cuisine" to "",
            "Difficulty" to "",
            "Ratings" to "",
            "Tags" to "",
            "Recency" to ""  // <-- New filter category
        )
    }

    var activeFilter by remember { mutableStateOf<String?>(null) } // Track which category is open
    val filteredRecipes by remember(searchQuery, selectedFilters, allRecipes) {
        derivedStateOf {
            applyFilters(allRecipes, searchQuery, selectedFilters)
                .sortedByDescending { recipe ->
                    // Parse the createdAt string and return the epoch seconds (or 0 if null)
                    recipe.createdAt?.let { parseCreatedAt(it)?.toEpochSecond() } ?: 0L
                }
        }
    }

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
    // Fetch all recipes
    LaunchedEffect(Unit) {
        recipeRepository.getAllRecipes { fetchedRecipes ->
            if (fetchedRecipes != null) {
                allRecipes.clear()
                allRecipes.addAll(fetchedRecipes)
            } else {
                println("Failed to fetch recipes for Discover page.")
            }
            isLoading = false
        }
    }

    var showAddOptionsModal by remember { mutableStateOf(false) }
    // Determine the current route to set selectedTab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTab = when (currentRoute) {
        "home" -> "home"
        "discover" -> "discover"
        else -> "home" // Default to "home" if route is unrecognized
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

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (currentRoute != tab) {
                        navController.navigate(tab) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onAddClick = { showAddOptionsModal = true }
            )
        }
    ) { paddingValues ->
        // 1) Use a Box to layer the background image and the content
        Box(modifier = Modifier.fillMaxSize()) {

            // 2) Background image
            Image(
                painter = painterResource(R.drawable.vava),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    // Add a blur effect, adjust the dp to control intensity
                    .blur(5.dp)
            )

            // 3) Foreground content (Column)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TopBar(navController)
                Spacer(modifier = Modifier.height(16.dp))
                TabButtons(
                    navController = navController,
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        if (currentRoute != tab) {
                            navController.navigate(tab) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                SearchAndFilterSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearchTriggered = { /* no-op, filtering is reactive */ },
                    selectedFilters = selectedFilters,
                    onFilterClick = { category -> activeFilter = category },
                    onRemoveFilter = { category -> selectedFilters[category] = "" }
                )


                // Show the modal if a filter is active
                FilterSelectionModal(
                    activeFilter = activeFilter,
                    onDismiss = { activeFilter = null },
                    onSelect = { category, value ->
                        selectedFilters[category] = value
                        activeFilter = null
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                // Content Section
                if (isLoading) {
                    LoadingIndicators()
                } else {
                    if (filteredRecipes.isEmpty()) {
                        Text(
                            text = "No recipes found",
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(filteredRecipes) { recipe ->
                                RecipeItemz(
                                    recipe = recipe,
                                    onClick = { navController.navigate("recipeDetail/${recipe.id}") },
                                    topWinners = topWinners
                                )
                            }
                        }

                    }
                }
            }
        }
    }

}

@Composable
fun LoadingIndicators() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}
fun applyFilters(
    allRecipes: List<Recipe>,
    searchQuery: String,
    selectedFilters: Map<String, String>
): List<Recipe> {
    val query = searchQuery.lowercase()
    return allRecipes.filter { recipe ->
        val isPublic = recipe.isPublic
        val matchesSearch = recipe.title.lowercase().contains(query) ||
                recipe.notes.lowercase().contains(query) ||
                recipe.tags.any { it.lowercase().contains(query) }


        // Cuisine filter
        val cuisineFilter = selectedFilters["Cuisine"].orEmpty().lowercase()
        val matchesCuisine = if (cuisineFilter.isNotEmpty()) {
            recipe.cuisine.lowercase() == cuisineFilter ||
                    recipe.tags.any { it.lowercase() == cuisineFilter }
        } else true

        // Difficulty filter
        val difficultyFilter = selectedFilters["Difficulty"].orEmpty().lowercase()
        val matchesDifficulty = if (difficultyFilter.isNotEmpty()) {
            recipe.difficulty.lowercase() == difficultyFilter ||
                    recipe.tags.any { it.lowercase() == difficultyFilter }
        } else true

        // Ratings filter – check for an exact match by comparing rounded values
        val ratingsFilter = selectedFilters["Ratings"].orEmpty()
        val matchesRatings = if (ratingsFilter.isNotEmpty()) {
            val selectedRating = ratingsFilter.toIntOrNull() ?: 0
            round(recipe.averageRating).toInt() == selectedRating
        } else true


        // Tags filter
        val tagsFilter = selectedFilters["Tags"].orEmpty().lowercase()
        val matchesTags = if (tagsFilter.isNotEmpty()) {
            recipe.tags.any { it.lowercase() == tagsFilter }
        } else true
        val recencyFilter = selectedFilters["Recency"].orEmpty()
        val matchesRecency = if (recencyFilter.isNotEmpty() && recipe.createdAt != null) {
            // Parse the recipe's creation date. (Ensure parseCreatedAt returns a LocalDateTime.)
            val createdTime = parseCreatedAt(recipe.createdAt) ?: return@filter false
            val now = OffsetDateTime.now()
            when (recencyFilter) {
                "Last 24 Hours" -> createdTime.isAfter(now.minusDays(1))
                "Last 7 Days" -> createdTime.isAfter(now.minusDays(7))
                "Last 30 Days" -> createdTime.isAfter(now.minusDays(30))
                else -> true
            }
        } else true
        isPublic && matchesSearch && matchesCuisine && matchesDifficulty && matchesRatings && matchesTags && matchesRecency
    }
}

@Composable
fun RecipeCardz(
    recipe: Recipe,
    onClick: () -> Unit,
    winnerRank: Int? = null // Pass the rank if available
) {
    // Create a modifier for the border if the recipe’s author is a top winner.
    val borderModifier = if (winnerRank != null) {
        // Choose border color based on rank:
        val borderColor = when (winnerRank) {
            1 -> Color(0xFFFFD700) // Gold
            2 -> Color(0xFFC0C0C0) // Silver
            3 -> Color(0xFFCD7F32) // Bronze
            else -> Color.Transparent
        }
        // Animate the border’s alpha for a glowing effect.
        val infiniteTransition = rememberInfiniteTransition()
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            )
        )
        Modifier.border(
            BorderStroke(4.dp, borderColor.copy(alpha = animatedAlpha)),
            shape = RoundedCornerShape(16.dp)
        )
    } else Modifier

    Card(
        modifier = Modifier
            .then(borderModifier)
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Recipe image
                Image(
                    painter = rememberAsyncImagePainter(model = recipe.imageUri),
                    contentDescription = recipe.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Recipe title
                Text(
                    text = recipe.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                // Recipe description
                Text(
                    text = recipe.notes,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            // Author name at bottom right (optional)
            recipe.author.takeIf { it.isNotBlank() }?.let { authorName ->
                Text(
                    text = authorName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
        }
    }
}


@Composable
fun RecipeItemz(
    recipe: Recipe,
    onClick: () -> Unit,
    topWinners: List<FeaturedWinner>
) {
    val computedRank = topWinners.indexOfFirst { winner ->
        // Adjust this comparison if needed – try comparing username instead of email:
        winner.userEmail.equals(recipe.author, ignoreCase = true) ||
                (winner.username?.equals(recipe.author, ignoreCase = true) ?: false)
    }
    val winnerRank = if (computedRank in 0..2) computedRank + 1 else null
    Log.d("RecipeItemz", "Recipe: ${recipe.title}, author: ${recipe.author}, computedRank: $computedRank, winnerRank: $winnerRank")

    Column(modifier = Modifier.fillMaxWidth()) {
        RecipeCardz(recipe = recipe, onClick = onClick, winnerRank = winnerRank)
        Spacer(modifier = Modifier.height(4.dp))
        recipe.createdAt?.let { created -> RelativeTimeText(created) }
        Spacer(modifier = Modifier.height(8.dp))
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchTriggered: () -> Unit,
    selectedFilters: Map<String, String>,
    onFilterClick: (String) -> Unit,
    onRemoveFilter: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = {
                onSearchQueryChange(it)
                onSearchTriggered()
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            placeholder = {
                Text("Search a recipe ...", color = Color(0xFF9C9C9C))
            },
            textStyle = TextStyle(color = Color(0xFFBBBBBB)),
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(0xFF2E2E2E),
                cursorColor = Color.White,
                unfocusedLabelColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Filter Chips Row
        FilterChips(
            selectedFilters = selectedFilters,
            onFilterClick = onFilterClick,
            onRemoveFilter = onRemoveFilter
        )
    }
}


@Composable
fun FilterChips(
    selectedFilters: Map<String, String>,
    onFilterClick: (String) -> Unit,
    onRemoveFilter: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        selectedFilters.forEach { (category, value) ->
            val chipText = if (value.isNotEmpty()) "$category: $value" else category
            FilterChip(
                text = chipText,
                onClick = { onFilterClick(category) },
                onRemove = if (value.isNotEmpty()) { { onRemoveFilter(category) } } else null
            )
        }
    }
}

@Composable
fun FilterChip(text: String, onClick: () -> Unit, onRemove: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x6A886332))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = text, color = Color(0xFFBBBBBB), fontSize = 16.sp)
            if (onRemove != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel), // Ensure you have an "X" icon resource.
                    contentDescription = "Remove Filter",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onRemove() }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSelectionModal(
    activeFilter: String?,
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit
) {
    if (activeFilter == null) return

    // Define available options for each filter category
    val options = when (activeFilter) {
        "Cuisine" -> listOf( "Russian",
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
            "Vietnamese")
        "Difficulty" -> listOf("Easy", "Medium", "Hard")
        "Ratings" -> listOf("1", "2", "3", "4", "5")
        "Tags" -> listOf("Vegan",
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
            "Holiday")
        "Recency" -> listOf("Last 24 Hours", "Last 7 Days", "Last 30 Days")
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select $activeFilter",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        },
        text = {
            // Wrap the options in a vertical scroll
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(activeFilter, option) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF333333))
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(16.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = Color(0xFF886332))
            }
        },
        containerColor = Color(0xFF1F1F1F),
        shape = RoundedCornerShape(16.dp)
    )
}
