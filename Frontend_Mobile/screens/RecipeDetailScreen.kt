package com.example.recipeapp.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.Book
import com.example.recipeapp.utils.BookRepository
import com.example.recipeapp.utils.CreateReviewDTO
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.Review
import com.example.recipeapp.utils.ReviewDTO
import com.example.recipeapp.utils.ReviewItem
import com.example.recipeapp.utils.ShoppingRepository
import com.example.recipeapp.utils.StarRatingDisplay
import com.example.recipeapp.utils.StarRatingInput
import com.example.recipeapp.utils.UserDTO
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.example.recipeapp.network.RecipeService

import com.example.recipeapp.utils.RecipeReportDTO
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


private val LightColors = lightColorScheme(
    primary = Color(0xFF00897B),
    secondary = Color(0xFF886332),
    background = Color.Black,
    surface = Color.DarkGray,
    onPrimary = Color.White,
    onBackground = Color.White,
    // Add more as needed
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    navController: NavController,
    recipes: MutableList<Recipe>,
    shoppingRepository: ShoppingRepository, // New parameter
    onAddIngredientsToShoppingList: (List<String>) -> Unit// New parameter
) {
    val context = LocalContext.current
    val token = AuthPreferences.getToken(context) ?: ""
    val recipeRepository = remember { RecipeRepository(context) }
    // Retrieve current user's username and ID
    val currentUsername = AuthPreferences.getUsername(context) ?: ""
    val currentUserId = AuthPreferences.getUserId(context) ?: 0L
    val bookRepository = remember { BookRepository(context) }
    var recipe by remember { mutableStateOf<Recipe?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFavorited by remember { mutableStateOf(false) }


    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var averageRating by remember { mutableStateOf(0f) }

    // State for posting a new review
    var userRating by remember { mutableStateOf(0) }
    var userComment by remember { mutableStateOf("") }
    var isPostingReview by remember { mutableStateOf(false) }
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
    // Fetch recipe details when the screen is launched
    LaunchedEffect(recipeId) {
        recipeRepository.getRecipeById(recipeId) { fetchedRecipe ->
            if (fetchedRecipe != null) {
                Log.d("RecipeDetailScreen", "Fetched Recipe: $fetchedRecipe")
                Log.d("RecipeDetailScreen", "Fetched Reviews: ${fetchedRecipe.reviews}")
            } else {
                Log.e("RecipeDetailScreen", "Failed to fetch recipe.")
            }
            recipe = fetchedRecipe
            isLoading = false
            if (fetchedRecipe == null) {
                errorMessage = "Failed to load recipe details."
            } else {
                // Check if the recipe is favorited
                recipeRepository.isRecipeFavorited(recipeId) { favorited ->
                    isFavorited = favorited
                }
            }
        }
    }


    if (isLoading) {
        // Show a loading indicator
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF876232))
        }
    } else if (errorMessage != null) {
        // Show error message
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = errorMessage ?: "An unknown error occurred.",
                color = Color.Red,
                fontSize = 16.sp
            )
        }
    } else if (recipe != null) {
        // Display the recipe details
        val currentUserId = AuthPreferences.getUserId(context) ?: 0L
        val isAuthor = recipe!!.authorId == currentUserId
        var showBookSelectionSheet by remember { mutableStateOf(false) }
        if (isLoading) {
            // Show a loading indicator
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF876232))
            }
        } else {
            RecipeDetailContent(
                recipe = recipe!!,
                recipeId = recipeId,
                recipes = recipes,
                navController = navController,
                isAuthor = isAuthor,
                isFavorited = isFavorited, // Pass the favorite status
                onFavoriteToggle = { newStatus ->
                    isFavorited = newStatus // Update the UI state
                },// Pass the flag
                onDeleteRecipe = {
                    recipeRepository.deleteRecipe(recipeId, token) { success ->
                        if (success) {
                            Toast.makeText(
                                context,
                                "Recipe deleted successfully.",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            navController.navigate("home")
                        } else {
                            Toast.makeText(context, "Failed to delete recipe.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                },
                onAddIngredientsToShoppingList = { ingredients ->
                    onAddIngredientsToShoppingList(ingredients) // Correct callback invocation
                    navController.navigate("shopping") // Navigate to shopping screen after adding
                },
                recipeRepository = recipeRepository,
                onAddToBooksClick = {
                    // When the user taps the icon, we show the bottom sheet:
                    showBookSelectionSheet = true
                }
            )
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
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailContent(
    recipe: Recipe,
    recipes: MutableList<Recipe>,
    recipeId: Long,
    navController: NavController,
    onDeleteRecipe: () -> Unit,
    isAuthor: Boolean,
    isFavorited: Boolean,
    onFavoriteToggle: (Boolean) -> Unit,
    onAddIngredientsToShoppingList: (List<String>) -> Unit,
    recipeRepository: RecipeRepository,
    onAddToBooksClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("home") }
    var showNutrientPopup by remember { mutableStateOf(false) }
    val displaySource = getDomainName(recipe.source)
    val displayVideo = getDomainName(recipe.video)
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    var isPostingReview by remember { mutableStateOf(false) }

    // Initialize reviews and averageRating once from the recipe
    // RecipeDetailContent
    val reviewsList = recipe.reviews ?: emptyList()
    val focusManager = LocalFocusManager.current
// Then use 'reviewsList' instead of 'recipe.reviews'
    var reviews by remember {
        mutableStateOf(
            reviewsList.sortedByDescending { it.createdAt }
        )
    }
    var averageRating by remember {
        mutableStateOf(
            if (reviewsList.isNotEmpty()) {
                reviewsList.map { it.rating }.average().toFloat()
            } else 0f
        )
    }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var isReported by remember { mutableStateOf(false) }

    // State for posting a new review
    var userRating by remember { mutableStateOf(0) }
    var userComment by remember { mutableStateOf("") }
    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }
    var showAddOptionsModal by remember { mutableStateOf(false) }
    var authorDetails by remember { mutableStateOf<UserDTO?>(null) }
    var discoverRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    val authService = ApiClient.getAuthService(context)
    var loggedInUserId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        authService.getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        loggedInUserId = user.id // Store the logged-in user ID

                        // Fetch discover recipes only if the recipe is NOT made by the logged-in user
                        if (recipe.authorId != user.id) {
                            ApiClient.getRecipeService(context).getAllRecipes()
                                .enqueue(object : Callback<List<Recipe>> {
                                    override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                                        if (response.isSuccessful) {
                                            val filteredRecipes = response.body()
                                                ?.filter {
                                                    it.id != recipeId && // Exclude the current recipe
                                                            it.authorId != user.id && // Exclude logged-in user's recipes
                                                            it.isPublic // Only include public recipes
                                                }
                                                ?.shuffled() // Shuffle the list randomly
                                                ?.take(4) // Pick only 4 random recipes
                                                ?: emptyList()
                                            discoverRecipes = filteredRecipes
                                        } else {
                                            Log.e("DiscoverRecipes", "Failed to fetch recipes: ${response.code()} ${response.message()}")
                                        }
                                    }

                                    override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                                        Log.e("DiscoverRecipes", "Error fetching recipes: ${t.localizedMessage}")
                                    }
                                })
                        }
                    }
                } else {
                    Log.e("RecipeDetail", "Failed to fetch profile: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("RecipeDetail", "Error fetching profile: ${t.localizedMessage}")
            }
        })
    }

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
    // Show the modal bottom sheet when the Add button is clicked
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
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Add the logo to the left of the title
                            Icon(
                                painter = painterResource(id = R.drawable.ic_loogo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(48.dp), // Adjust size as needed
                                tint = Color(0xFF886332)
                            )
                            Spacer(modifier = Modifier.width(8.dp)) // Add some spacing between the logo and title
                            Text("LeGourmand", fontSize = 20.sp, color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        // Settings Icon aligned to the right
                        Text(
                            text = "Upgrade",
                            modifier = Modifier
                                .clickable { navController.navigate("premium") }
                                .padding(13.dp),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Cursive,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White
                    )
                )
                // Add a Divider as a bottom border
                Divider(
                    color = Color(0xFF474545), // Choose the color of the border
                    thickness = 1.dp, // Set the thickness of the border
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onAddClick = { showAddOptionsModal = true  }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            item {
                Log.d("RecipeDetailScreen", "Displaying Recipe - Tags: ${recipe.tags}, Difficulty: ${recipe.difficulty}, Cuisine: ${recipe.cuisine}")

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp), // Increased padding
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    // **Fix: Add padding inside Box to create spacing from edges**
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                            .padding(5.dp) // NEW padding added inside Box
                    ) {
                        // Display the image or placeholder
                        if (!recipe.imageUri.isNullOrEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(model = Uri.parse(recipe.imageUri)),
                                contentDescription = "Recipe Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Placeholder Icon",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(64.dp)
                            )
                        }

                        // Overlay row of icons at the top of the image
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp) // Reduce height to make the overlay smaller
                                .background(Color.Black.copy(alpha = 0.4f)) // Less opacity
                                .padding(horizontal = 2.dp) // Small horizontal padding
                                .align(Alignment.TopCenter) // Position the row at the top center
                        ) {
                            if (isAuthor) {
                                // Show Edit Button
                                IconButton(onClick = {

                                    recipe.id?.let { id ->
                                        navController.navigate("editRecipe/$id")
                                    } ?: run {
                                        Toast.makeText(
                                            context,
                                            "Invalid Recipe ID",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }

                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Show Delete Button
                                IconButton(onClick = {
                                    showDeleteConfirmation =
                                        true // Show confirmation dialog on delete click
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    onAddToBooksClick()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.AddBox,
                                        contentDescription = "Add to books",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            if (!isAuthor) {
                                IconButton(onClick = {
                                    onAddToBooksClick()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.AddBox,
                                        contentDescription = "Add to books",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Favorites Button
                            IconButton(onClick = {
                                if (isFavorited) {
                                    // Remove from favorites
                                    recipeRepository.removeFavoriteRecipe(
                                        recipe.id ?: 0L
                                    ) { success ->
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "Removed from favorites",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onFavoriteToggle(false)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to remove from favorites",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else {
                                    // Add to favorites
                                    recipeRepository.addFavoriteRecipe(recipe.id ?: 0L) { success ->
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "Added to favorites",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onFavoriteToggle(true)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to add to favorites",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (isFavorited) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = if (isFavorited) "Remove from favorites" else "Add to favorites",
                                    tint = if (isFavorited) Color.Yellow else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                    }

                    if (showDeleteConfirmation) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmation = false },
                            title = {
                                Text(
                                    text = "Delete Recipe",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            text = {
                                Text(
                                    text = "Are you sure you want to delete this recipe?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onDeleteRecipe() // Calls the delete function
                                        showDeleteConfirmation = false
                                        navController.popBackStack() // Navigate back after deletion
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Yes, Delete")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { showDeleteConfirmation = false },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancel")
                                }
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }


                    Spacer(modifier = Modifier.height(16.dp))
                    LaunchedEffect(recipe.authorId) {
                        Log.d(
                            "RecipeDetail",
                            "Attempting to fetch author details for ID: ${recipe.authorId}"
                        )
                        recipe.authorId?.let { id ->
                            ApiClient.getAuthService(context).getUserById(id)
                                .enqueue(object : Callback<UserDTO> {
                                    override fun onResponse(
                                        call: Call<UserDTO>,
                                        response: Response<UserDTO>
                                    ) {
                                        if (response.isSuccessful) {
                                            authorDetails = response.body()
                                            Log.d(
                                                "RecipeDetail",
                                                "Fetched author details: $authorDetails"
                                            )
                                        } else {
                                            Log.e(
                                                "RecipeDetail",
                                                "Error fetching author details: ${response.code()} ${response.message()}"
                                            )
                                        }
                                    }

                                    override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                                        Log.e(
                                            "RecipeDetail",
                                            "Error fetching author details: ${t.localizedMessage}"
                                        )
                                    }
                                })
                        }
                    }
                    // Title, Author
                    Text(
                        text = recipe.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center // Aligns the text within the full width
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    // Author and Ratings Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Create a clickable Row for author info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    recipe.authorId?.let { authorId ->
                                        navController.navigate("authorDetail/$authorId")
                                    }
                                }
                        ) {
                            // Display the author's profile picture or fallback to initials
                            if (authorDetails != null) {
                                if (!authorDetails!!.imageUri.isNullOrBlank()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = authorDetails!!.imageUri),
                                        contentDescription = "Author Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    // Fallback: display a circle with the author's initials
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = authorDetails!!.username?.take(2)
                                                    ?.uppercase() ?: "NN",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            } else {
                                // While author details are loading, show a placeholder image
                                Image(
                                    painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                                    contentDescription = "Placeholder Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Display the author's name
                            Text(
                                text = "Author: ${recipe.author}",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        // Ratings
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            StarRatingDisplay(rating = averageRating)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${String.format("%.1f", averageRating)} Stars",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))


                    val formattedDate = remember(recipe.createdAt) {
                        recipe.createdAt?.let {
                            try {
                                // Define a parser for the ISO 8601 format used in your createdAt string.
                                val parser = SimpleDateFormat(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                                    Locale.getDefault()
                                )
                                val date = parser.parse(it)
                                // Define a formatter for your desired output format.
                                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                formatter.format(date)
                            } catch (e: Exception) {
                                it // Fallback to the raw string if parsing fails.
                            }
                        } ?: ""
                    }

// Display creation date if available
                    if (!formattedDate.isNullOrEmpty()) {
                        Text(
                            text = "Created on: $formattedDate",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Difficulty and Cuisine Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star, // Use an appropriate icon for difficulty
                            contentDescription = "Difficulty Icon",
                            tint = Color(0xFF00897B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Difficulty: ",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = recipe.difficulty,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant, // Use an appropriate icon for cuisine
                            contentDescription = "Cuisine Icon",
                            tint = Color(0xFF00897B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Cuisine: ",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = recipe.cuisine,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val servingsInt = recipe.servings.toIntOrNull() ?: 0

                    Text(
                        "Servings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (servingsInt > 0) {
                        Text("$servingsInt servings", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        Text("No Servings to be made", color = Color.Gray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Notes Section with Green Circle Labels
                    if (recipe.notes.isNotEmpty()) {
                        Text(
                            text = "Notes",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        recipe.notes.split("\n").forEach { note ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            Color(0xFF886332),
                                            shape = CircleShape
                                        ) // Green circle
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = note,
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Prep and Cook Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Prep time",
                            tint = Color.Gray
                        )
                        Text("Prep: ${recipe.prepTime} mins", color = Color.Gray, fontSize = 14.sp)
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Cook time",
                            tint = Color.Gray
                        )
                        Text("Cook: ${recipe.cookTime} mins", color = Color.Gray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // URL, Video, and Nutrients Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Source link or placeholder text
                        Text(
                            text = "Source:",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (recipe.source.isNotEmpty() && android.util.Patterns.WEB_URL.matcher(
                                recipe.source
                            ).matches()
                        ) {
                            ClickableText(
                                text = AnnotatedString(getShortenedUrl(recipe.source)),
                                onClick = {
                                    openUrlInBrowser(
                                        formatUrl(recipe.source),
                                        navController
                                    )
                                },
                                modifier = Modifier.padding(top = 4.dp),
                                style = TextStyle(
                                    color = Color(0xFF00897B),
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        } else {
                            Text(
                                text = "No Source",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Video link or placeholder text
                        Text(
                            text = "Video:",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (recipe.video.isNotEmpty() && android.util.Patterns.WEB_URL.matcher(
                                recipe.video
                            ).matches()
                        ) {
                            ClickableText(
                                text = AnnotatedString(getShortenedUrl(recipe.video)),
                                onClick = {
                                    openUrlInBrowser(
                                        formatUrl(recipe.video),
                                        navController
                                    )
                                },
                                modifier = Modifier.padding(top = 4.dp),
                                style = TextStyle(
                                    color = Color(0xFF00897B),
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        } else {
                            Text(
                                text = "No Video",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nutrients Section
                    // Nutrients Section Header
                    Text(
                        text = "Nutrients",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(vertical = 0.dp)
                            .clickable { showNutrientPopup = true } // Show popup on click
                    )
                    Text(
                        text = "Click for more informations.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

// Floating Nutrient Popup with Animation
                    AnimatedVisibility(
                        visible = showNutrientPopup,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight }
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight }
                        ) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { showNutrientPopup = false },
                            contentAlignment = Alignment.Center
                        ) {
                            // Prevent clicks inside the Column from closing the popup
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .background(Color.DarkGray, shape = RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                                    .clickable(enabled = false) { },
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Nutrients",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Calories: ${recipe.calories} kcal",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Protein: ${recipe.protein} g",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Carbohydrates: ${recipe.carbohydrates} g",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Fat: ${recipe.fat} g",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sugar: ${recipe.sugar} g",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showNutrientPopup = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF00897B
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Close", color = Color.White)
                                }
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    // Ingredients Section with Servings
                    Text(
                        "Ingredients",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    if (recipe.ingredients.isBlank()) {
                        Text("No ingredients listed", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        recipe.ingredients.split("\n").forEach { ingredient ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF886332), shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ingredient,
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val ingredientList = recipe.ingredients.split("\n").map { it.trim() }
                                .filter { it.isNotEmpty() }
                            onAddIngredientsToShoppingList(ingredientList)
                            navController.navigate("shopping")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to shopping list",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to shopping list", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Instructions",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (recipe.instructions.isBlank()) {
                        Text(
                            "No instructions provided",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        // Display each instruction as a separate step
                        recipe.instructions.split("\n").forEachIndexed { index, instruction ->
                            Text(
                                text = "Step ${index + 1}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = instruction,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material.Divider(
                        color = Color.White.copy(alpha = 0.6f),
                        thickness = 1.dp
                    )


                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    focusManager.clearFocus() // Dismiss keyboard when tapping outside
                                })
                            }
                    ) {
                        // Post a review section with proper keyboard handling
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                                .imePadding(), // Moves content above the keyboard
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Post a review",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Star Rating Input
                            StarRatingInput(
                                rating = userRating,
                                onRatingChanged = { userRating = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Comment Input Field with keyboard-aware padding
                            OutlinedTextField(
                                value = userComment,
                                onValueChange = { userComment = it },
                                label = { Text("Write a comment...", color = Color.Gray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .imePadding(), // Ensures it moves up when the keyboard appears
                                textStyle = TextStyle(color = Color.White),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedLabelColor = Color.Gray,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedBorderColor = Color.Gray,
                                    unfocusedBorderColor = Color.Gray,
                                    cursorColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Post Comment Button
                            Button(
                                onClick = {
                                    if (userRating == 0) {
                                        Toast.makeText(
                                            context,
                                            "Please select a rating.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }
                                    if (userComment.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Please enter a comment.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }
                                    isPostingReview = true
                                    val createReviewDTO = CreateReviewDTO(
                                        rating = userRating,
                                        comment = userComment
                                    )
                                    recipeRepository.addReview(
                                        recipeId,
                                        createReviewDTO
                                    ) { newReview ->
                                        isPostingReview = false
                                        if (newReview != null) {
                                            reviews =
                                                (reviews + newReview).sortedByDescending { it.createdAt }
                                            averageRating = if (reviews.isNotEmpty()) {
                                                reviews.map { it.rating }.average().toFloat()
                                            } else {
                                                0f
                                            }
                                            userRating = 0
                                            userComment = ""
                                            Toast.makeText(
                                                context,
                                                "Review added successfully.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to add review.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF00897B
                                    )
                                ),
                                enabled = !isPostingReview,
                            ) {
                                if (isPostingReview) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 8.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                Text("Posting Review", fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Display Reviews
                    Text(
                        "Reviews",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    if (reviews.isEmpty()) {
                        Text("No reviews yet", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        reviews.forEach { review ->
                            ReviewItem(
                                review = review,
                                recipe = recipe
                            ) // pass the recipe instance, not the Recipe type
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    androidx.compose.material.Divider(
                        color = Color.White.copy(alpha = 0.6f),
                        thickness = 1.dp
                    )

                    if (loggedInUserId != null && recipe.authorId != loggedInUserId) { // Only show if the logged-in user is NOT the author
                        Spacer(modifier = Modifier.height(32.dp))

                        // Section Title
                        Text(
                            text = "Discover Recipes",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (discoverRecipes.isEmpty()) {
                            Text("No recipes available", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                for (i in discoverRecipes.chunked(2)) { // Groups the recipes into pairs
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        i.forEach { recipe ->
                                            DiscoverRecipeCard(recipe = recipe, navController = navController, modifier = Modifier.weight(1f))
                                        }
                                        if (i.size == 1) Spacer(modifier = Modifier.weight(1f)) // Filler if only one item in row
                                    }
                                }
                            }
                        }
                    }

                    // Tags Section with Green Circle Labels
                    if (recipe.isAiGenerated) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                                .border(1.dp, Color.Gray, shape = RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Generated by the LeGourmand AI.",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = """
                This recipe is AI-generated and LeGourmand has not reviewed it for accuracy or safety.
                Use your best judgment when preparing AI-generated dishes.
                Please rate this recipe to help others know if it's good or not.
                """.trimIndent(),
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Justify
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (recipe.tags.isNotEmpty()) {
                        Text(
                            text = "Tags",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recipe.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color(0xFF00897B),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isAuthor) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable {
                                    // Open the report dialog only if not already reported
                                    if (!isReported) {
                                        showReportDialog = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "You have already reported this recipe.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Report Recipe",
                                tint = Color(0xB0878787),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Report Recipe",
                                color = Color(0xB0878787),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (showReportDialog) {
                        AlertDialog(
                            onDismissRequest = { showReportDialog = false },
                            containerColor = Color(0xFF1E1E1E), // Dark background for a modern look
                            shape = RoundedCornerShape(20.dp),
                            title = {
                                Text(
                                    text = "Report Recipe",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFF9800) // Vibrant orange accent
                                )
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Enter a reason for reporting:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFE0E0E0)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = reportReason,
                                        onValueChange = { reportReason = it },
                                        label = {
                                            Text(
                                                text = "Reason",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFB0BEC5)
                                            )
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(color = Color(0xFFE0E0E0)),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0xFFFF9800),
                                            unfocusedBorderColor = Color(0xFF757575),
                                            cursorColor = Color(0xFFFF9800),
                                            focusedLabelColor = Color(0xFFFF9800),
                                            unfocusedLabelColor = Color(0xFFB0BEC5)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        // Call your helper function to report the recipe.
                                        reportRecipe(recipe.id ?: 0L, reportReason, context) { success, alreadyReported ->
                                            if (success) {
                                                Toast.makeText(
                                                    context,
                                                    "Recipe reported successfully.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                isReported = true
                                            } else if (alreadyReported) {
                                                Toast.makeText(
                                                    context,
                                                    "You have already reported this recipe.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                isReported = true
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to report recipe.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        showReportDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(text = "Report", color = Color.White, style = MaterialTheme.typography.labelLarge)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { showReportDialog = false },
                                    border = BorderStroke(1.dp, Color(0xFFFF9800)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(text = "Cancel", color = Color(0xFFFF9800), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        )
                    }

                }
            }
        }
        // Bottom left "Report Recipe" action


    }
}
fun reportRecipe(recipeId: Long, reason: String, context: Context, callback: (Boolean, Boolean) -> Unit) {
    // Example: create your API service from Retrofit.
    val apiService = ApiClient.getRetrofit(context).create(RecipeService::class.java)
    apiService.reportRecipe(recipeId, reason)
        .enqueue(object : Callback<RecipeReportDTO> {
            override fun onResponse(call: Call<RecipeReportDTO>, response: Response<RecipeReportDTO>) {
                if (response.isSuccessful) {
                    callback(true, false)
                } else {
                    // For instance, if status code 409 indicates a duplicate report:
                    if (response.code() == 409) {
                        callback(false, true)
                    } else {
                        callback(false, false)
                    }
                }
            }
            override fun onFailure(call: Call<RecipeReportDTO>, t: Throwable) {
                callback(false, false)
            }
        })
}


// Helper Functions

fun formatUrl(url: String): String {
    return if (!url.startsWith("http")) {
        "https://$url"
    } else {
        url
    }
}

fun getShortenedUrl(url: String, maxLength: Int = 30): String {
    return try {
        val formattedUrl = Uri.parse(url).host ?: url
        if (formattedUrl.length > maxLength) {
            "${formattedUrl.take(maxLength)}..."
        } else {
            formattedUrl
        }
    } catch (e: Exception) {
        if (url.length > maxLength) "${url.take(maxLength)}..." else url
    }
}

fun getDomainName(url: String): String {
    val formattedUrl = if (!url.startsWith("http")) {
        "https://$url"
    } else {
        url
    }

    return try {
        val uri = Uri.parse(formattedUrl)
        uri.host?.replaceFirst("www.", "") ?: url
    } catch (e: Exception) {
        url
    }
}

// Function to handle URL opening and log if no activity is found
fun openUrlInBrowser(url: String, navController: NavController) {
    val context = navController.context
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Log.e("RecipeDetailScreen", "No activity found to handle URL: $url")
        Toast.makeText(context, "No app found to open this link.", Toast.LENGTH_SHORT).show()
    }
}
@Composable
fun BookSelectionBottomSheet(
    recipe: Recipe,
    bookRepository: BookRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Store the user’s books
    var userBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    // To show a loading indicator while we fetch
    var isLoading by remember { mutableStateOf(true) }

    // Fetch the user's books only once
    LaunchedEffect(Unit) {
        bookRepository.getUserBooks { books ->
            if (books != null) {
                userBooks = books
            }
            isLoading = false
        }
    }

    // We'll display a list of the user’s books with a check if the recipe is in it
    Column(
        modifier = Modifier
            .fillMaxWidth()

            .padding(16.dp)
    ) {
        // Header row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Select a Book",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            // Close icon
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF876232))
            }
        } else {
            if (userBooks.isEmpty()) {
                Text(
                    text = "No cookbooks found. Create one first.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                // Show a list of books
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(userBooks) { book ->
                        BookSelectionItem(
                            book = book,
                            recipe = recipe,
                            bookRepository = bookRepository
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookSelectionItem(
    book: Book,
    recipe: Recipe,
    bookRepository: BookRepository
) {
    val context = LocalContext.current

    // Keep track in Compose state, ignoring what's in book.recipeIds.
    var recipeIsInBook by remember { mutableStateOf(book.recipeIds.contains(recipe.id)) }
    var isToggling by remember { mutableStateOf(false) }

    Card(
        // ...
        modifier = Modifier.clickable(enabled = !isToggling) {
            if (recipe.id == null) return@clickable
            isToggling = true
            if (recipeIsInBook) {
                bookRepository.removeRecipeFromBook(book.id ?: 0L, recipe.id) { success ->
                    if (success) {
                        Toast.makeText(context, "Removed from '${book.title}'", Toast.LENGTH_SHORT).show()
                        recipeIsInBook = false // <-- Re-assign local state
                    }
                    isToggling = false
                }
            } else {
                bookRepository.addRecipeToBook(book.id ?: 0L, recipe.id) { success ->
                    if (success) {
                        Toast.makeText(context, "Added to '${book.title}'", Toast.LENGTH_SHORT).show()
                        recipeIsInBook = true  // <-- Re-assign local state
                    }
                    isToggling = false
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Title
            Text(
                text = book.title,
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            // Show an icon if it's in the book => check, else a plus
            if (recipeIsInBook) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Recipe is in this book",
                    tint = Color(0xFF4CAF50)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Add recipe to this book",
                    tint = Color.Gray
                )
            }
        }
    }
}
@Composable
fun DiscoverRecipeCard(recipe: Recipe, navController: NavController, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(200.dp) // Adjusted for 2x2 layout
            .clickable { navController.navigate("recipeDetail/${recipe.id}") },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray)
            ) {
                if (!recipe.imageUri.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = recipe.imageUri),
                        contentDescription = "Recipe Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "No Image",
                        modifier = Modifier.align(Alignment.Center),
                        tint = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(recipe.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            StarRatingDisplay(recipe.averageRating ?: 0f)
        }
    }
}

