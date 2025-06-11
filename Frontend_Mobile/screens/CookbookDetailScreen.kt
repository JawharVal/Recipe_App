package com.example.recipeapp.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.BottomNavigationBar

import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.Book
import com.example.recipeapp.utils.BookRepository
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookDetailScreen(navController: NavController, bookId: Long, recipes: MutableList<Recipe>) {
    val context = LocalContext.current
    val bookRepository = remember { BookRepository(context) }
    val recipeRepository = remember { RecipeRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = AuthPreferences.getUserId(context)
    var favoriteRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }

    var showAddOptionsModal by remember { mutableStateOf(false) }
    var book by remember { mutableStateOf<Book?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddRecipesDialog by remember { mutableStateOf(false) }
    var allRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var isLoadingRecipes by remember { mutableStateOf(true) }
    val isOwnCookbook = book?.authorId == currentUserId


    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isSheetVisible by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Subscription state
    // Instead of a derived state, we use a mutable state that can be updated from the SearchBar
    var displayedRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }

    // When the book or allRecipes change, reset displayedRecipes to all cookbook recipes
    LaunchedEffect(book, allRecipes) {
        displayedRecipes = book?.let { nonNullBook ->
            allRecipes.filter { it.id in nonNullBook.recipeIds }
        } ?: emptyList()
    }
    // Instead of a mutableStateListOf, use derivedStateOf so it updates automatically
    val filteredRecipes by remember(book, allRecipes) {
        derivedStateOf {
            book?.let { nonNullBook ->
                allRecipes.filter { it.id in nonNullBook.recipeIds }
            } ?: emptyList()
        }
    }

    val authService = ApiClient.getAuthService(context)
    // Subscription state (not shown fully here for brevity)
    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        recipeRepository.getFavoriteRecipes { favorites ->
            favoriteRecipes = favorites ?: emptyList()
        }
    }
    val allowedRecipes = allRecipes.filter { recipe ->
        recipe.authorId == currentUserId || favoriteRecipes.any { it.id == recipe.id }
    }

    // Fetch profile and update subscription state
    LaunchedEffect(Unit) {
        authService.getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        when (user.subscriptionType?.uppercase() ?: "FREE") {
                            "FREE" -> { plusUnlocked = false; proUnlocked = false }
                            "PLUS" -> { plusUnlocked = true; proUnlocked = false }
                            "PRO"  -> { plusUnlocked = true; proUnlocked = true }
                        }
                    }
                } else {
                    Log.e("CookbookDetailScreen", "Failed to fetch profile: ${response.code()} ${response.message()}")
                }
            }
            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("CookbookDetailScreen", "Error fetching profile: ${t.localizedMessage ?: "Unknown error"}")
            }
        })
    }

    // Fetch book details
    LaunchedEffect(bookId) {
        bookRepository.getPublicBookById(bookId) { fetchedBook ->
            if (fetchedBook != null) {
                book = fetchedBook
            } else {
                Toast.makeText(context, "Failed to load cookbook details.", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    // Fetch all recipes
    LaunchedEffect(Unit) {
        recipeRepository.getAllRecipes { fetchedRecipes ->
            if (fetchedRecipes != null) {
                allRecipes = fetchedRecipes.filter { it.id != null }
                isLoadingRecipes = false
            } else {
                Toast.makeText(context, "Failed to load recipes.", Toast.LENGTH_SHORT).show()
                isLoadingRecipes = false
            }
        }
    }


    Scaffold(
        topBar = {
            CookbookDetailTopBar(navController = navController, title = "LeGourmand")
        },
        floatingActionButton = {
            if (isOwnCookbook) {
                FloatingActionButton(
                    onClick = { showAddRecipesDialog = true },
                    containerColor = Color(0xFF886332)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Recipes",
                        tint = Color.Black
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedTab = "cookbooks",
                onTabSelected = { /* Handle tab change */ },
                onAddClick = { showAddOptionsModal = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            when {
                isLoading || isLoadingRecipes -> {
                    CircularProgressIndicator(color = Color(0xFF886332), modifier = Modifier.align(Alignment.Center))
                }
                book == null -> {
                    Text(
                        text = "Cookbook not found",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        // Book Title and Description
                        book?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = it.description ?: "No description available.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Search Bar: update displayedRecipes via onSearchResult
                        SearchBar(
                            recipes = book?.let { nonNullBook ->
                                allRecipes.filter { it.id in nonNullBook.recipeIds }
                            } ?: emptyList(),
                            onSearchResult = { filtered ->
                                displayedRecipes = filtered
                            },
                            availableTags = listOf("Vegan", "Dessert", "Quick Meals"),
                            availableCuisines = listOf("Italian", "Chinese", "Mexican"),
                            availableDifficulties = listOf("Easy", "Medium", "Hard")
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Recipes Grid
                        if (displayedRecipes.isEmpty()) {
                            EmptyCookbookDetailState(
                                isOwner = isOwnCookbook,  // ✅ Pass ownership status
                                onAddRecipesClick = { showAddRecipesDialog = true }
                            )
                        }
                        else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .background(Color(0xFF1B1B1B))
                            ) {
                                Text(
                                    text = "Recipes",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(displayedRecipes) { recipe ->
                                        RecipeCard(
                                            recipe = recipe,
                                            onClick = { navController.navigate("recipeDetail/${recipe.id!!}") },
                                            onDelete = {
                                                if (isOwnCookbook) {
                                                    coroutineScope.launch {
                                                        bookRepository.removeRecipeFromBook(bookId, recipe.id!!) { success ->
                                                            if (success) {
                                                                bookRepository.getBookById(bookId) { updatedBook ->
                                                                    book = updatedBook
                                                                }
                                                                Toast.makeText(context, "Recipe removed!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Failed to remove recipe.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            isOwner = isOwnCookbook
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add Recipes Dialog and Bottom Sheet code omitted for brevity…
            if (showAddRecipesDialog && !isLoadingRecipes) {
                AddRecipesDialog(
                    bookId = bookId,
                    onDismiss = { showAddRecipesDialog = false },
                    onAddRecipes = { selectedRecipeIds ->
                        coroutineScope.launch {
                            selectedRecipeIds.forEach { recipeId ->
                                bookRepository.addRecipeToBook(bookId, recipeId) { success ->
                                    if (success) {
                                        bookRepository.getBookById(bookId) { updatedBook ->
                                            book = updatedBook
                                        }
                                    }
                                }
                            }
                            showAddRecipesDialog = false
                        }
                    },
                    allRecipes = allowedRecipes,
                    favoriteRecipes = favoriteRecipes,
                    currentUserId = currentUserId
                )
            }
            // Handle Add Options Modal and Bottom Sheet (omitted for brevity)
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
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookDetailTopBar(navController: NavController, title: String) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Centered Title
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_loogo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF886332)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                fontSize = 20.sp,  // ✅ Font size applied
                                color = Color.White,  // ✅ White text color
                                  // Optional: Make it bold
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Black,
                titleContentColor = Color.White
            )
        )

        // Bottom Divider
        Divider(
            color = Color(0xFF474545),
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    isOwner: Boolean // Add a callback for deletion
) {
    var showDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDropdown = true } // Show dropdown on long press
            ),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (recipe.imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = recipe.imageUri),
                    contentDescription = "Recipe Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_loogo),
                        contentDescription = "Placeholder",
                        tint = Color.White
                    )
                }
            }
            Text(
                text = recipe.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Dropdown Menu
        if (isOwner) {
            Box(modifier = Modifier.fillMaxSize()) {
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove", color = Color.Red) },
                        onClick = {
                            onDelete() // ✅ Trigger delete action
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun EmptyCookbookDetailState(
    isOwner: Boolean, // ✅ Check if the user is the owner
    onAddRecipesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize() // 🔹 Ensures it takes up the whole screen
            .padding(top = 1.dp),
        verticalArrangement = Arrangement.Center, // 🔹 Centers content vertically
        horizontalAlignment = Alignment.CenterHorizontally // 🔹 Centers content horizontally
    ) {
        // ✅ Centered Title
        Text(
            text = if (isOwner) "Add Recipes" else "No Recipes Available :(",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center, // 🔹 Ensures text is centered
            modifier = Modifier.fillMaxWidth() // 🔹 Ensures proper centering
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isOwner) {
                "Use cookbooks to organize your recipes. You can also invite your friends and family to collaborate on cookbooks."
            } else {
                "This cookbook is currently empty."
            },
            color = Color.Gray,
            textAlign = TextAlign.Center, // 🔹 Centers the text
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(1.1f) // 🔹 Makes text width smaller for better readability
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ Only show the "Add Recipes" button if the user owns the cookbook
        if (isOwner) {
            Button(
                onClick = onAddRecipesClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A592D)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(horizontal = 16.dp) // 🔹 Adds spacing to button
            ) {
                Text(text = "Add Recipes", color = Color.White)
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    recipes: List<Recipe>,
    onSearchResult: (List<Recipe>) -> Unit, // This updates the filtered list
    availableTags: List<String>,
    availableCuisines: List<String>,
    availableDifficulties: List<String>
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { newQuery ->
                searchQuery = newQuery
                // Filter recipes based on searchQuery
                val filtered = if (searchQuery.isNotBlank()) {
                    recipes.filter { it.title.contains(searchQuery, ignoreCase = true) }
                } else {
                    recipes // Show all recipes if search is empty
                }
                onSearchResult(filtered)
            },
            placeholder = { Text("Search recipes...", color = Color.Gray) },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Search",
                    tint = Color.Gray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                keyboardType = KeyboardType.Text // Adjusts the type of keyboard
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(color = Color.White), // Correct way to set text color
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = Color.White,
                focusedBorderColor = Color(0xFF886332),
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White, // Use correct property
                unfocusedTextColor = Color.White // Ensure text is visible even when not focused
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}



@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOptions: SnapshotStateList<String>,
    onSelectionChange: (List<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.width(100.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        val newSelected = selectedOptions.toMutableList()
                        if (newSelected.contains(option)) {
                            newSelected.remove(option)
                        } else {
                            newSelected.add(option)
                        }
                        onSelectionChange(newSelected)
                        expanded = false // Close the dropdown after selection
                    },
                    trailingIcon = {
                        if (selectedOptions.contains(option)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Not Selected",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        }
    }
}

