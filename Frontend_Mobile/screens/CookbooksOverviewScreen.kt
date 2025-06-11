package com.example.recipeapp.screens


import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController

import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar

import com.example.recipeapp.ui.theme.RecipeAppTheme
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.Book
import com.example.recipeapp.utils.BookDTO
import com.example.recipeapp.utils.BookRepository
import com.example.recipeapp.utils.Note
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO
import com.example.recipeapp.utils.toDTO
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbooksOverviewScreen(navController: NavController, recipes: MutableList<Recipe>,subscriptionType: String) {
    val context = LocalContext.current
    val bookRepository = remember { BookRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddOptionsModal by remember { mutableStateOf(false) }

    var emptyInputText by remember { mutableStateOf("") }

    // State variables for adding items when list is not empty
    var addInputText by remember { mutableStateOf("") }

    var selectedTab by remember { mutableStateOf("shoppingList") }
    var sortAscending by remember { mutableStateOf(true) }
    var isSheetVisible by remember { mutableStateOf(false) }
    var sortRecipesAscending by remember { mutableStateOf(false) }

    // Manage planned recipes and notes
    val plannedRecipes = remember { mutableStateMapOf<LocalDate, SnapshotStateList<Recipe>>() }
    val notes = remember { mutableStateMapOf<LocalDate, SnapshotStateList<Note>>() }
    var sortingMode by remember { mutableStateOf(SortingMode.TITLE) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }
    val sortedBooks = when (sortingMode) {
        SortingMode.TITLE -> if (sortAscending) {
            books.sortedBy { it.title }
        } else {
            books.sortedByDescending { it.title }
        }
        SortingMode.RECIPES -> if (sortRecipesAscending) {
            books.sortedBy { it.recipeIds.size }
        } else {
            books.sortedByDescending { it.recipeIds.size }
        }
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var favoritesError by remember { mutableStateOf<String?>(null) }
    var isFavoritesLoading by remember { mutableStateOf(false) }

    // For demonstration, we also add user-related states.
    var user by remember { mutableStateOf<UserDTO?>(null) }
    var username by remember { mutableStateOf("User") }
    var favoritesCount by remember { mutableStateOf(0) }
    val recipeRepository = remember { RecipeRepository(context) }
    val authService = ApiClient.getAuthService(context)
    // Fetch subscription info as well as books
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

        val userId = AuthPreferences.getUserId(context)
        if (userId != null) {
            bookRepository.getBooksByUserId(userId) { fetchedBooks ->
                if (fetchedBooks == null) {
                    errorMessage = "Failed to load cookbooks."
                } else {
                    books = fetchedBooks
                }
                isLoading = false
            }
        } else {
            navController.navigate("login") {
                popUpTo("cookbooksOverview") { inclusive = true }
            }
        }

    }

    // Compute the current subscription type from local state.
    val currentSubscriptionType = when {
        proUnlocked -> "PRO"
        plusUnlocked -> "PLUS"
        else -> "FREE"
    }

    // Determine maximum allowed cookbooks based on the current subscription type.
    val maxbooksAllowed = when (currentSubscriptionType.uppercase()) {
        "FREE" -> 3
        "PLUS" -> 10
        "PRO" -> Int.MAX_VALUE
        else -> 10
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
    // ----------- NEW: Error Handling Block ------------- //
    if (errorMessage != null || favoritesError != null) {
        ErrorPageDarkz(
            message = errorMessage ?: favoritesError!!,
            navController = navController,
            onRetry = {
                // Retry logic:
                isLoading = true
                isFavoritesLoading = false
                errorMessage = null
                favoritesError = null
                fetchUserDetails(
                    context = context,
                    onSuccess = { fetchedUser ->
                        user = fetchedUser
                        username = fetchedUser.username ?: "User"
                        isLoading = false

                        // Retry fetching favorites
                        isFavoritesLoading = true
                        recipeRepository.getFavoriteRecipes { favorites ->
                            if (favorites != null) {
                                favoritesCount = favorites.size
                                isFavoritesLoading = false
                            } else {
                                favoritesError = "Failed to load favorites."
                                isFavoritesLoading = false
                            }
                        }
                    },
                    onFailure = {
                        errorMessage = "Failed to load user data."
                        isLoading = false
                    }
                )
            },
            onLogout = {
                AuthPreferences.clearToken(context)
                navController.navigate("login") {
                    popUpTo("profile") { inclusive = true }
                }
            }
        )
    } else {

    Scaffold(
        topBar = {
            CookbooksTopBar(
                onSettingsClick = { navController.navigate("editProfile") },
                navController = navController
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Use the computed books count and maxbooksAllowed
                    if (books.size >= maxbooksAllowed) {
                        Toast.makeText(
                            context,
                            "You have reached the maximum allowed number of books for the $currentSubscriptionType plan.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@FloatingActionButton
                    }
                    showCreateDialog = true
                },
                containerColor = Color(0xFF866232)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Cookbook")
            }
        },

        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedTab = "cookbooks",
                onTabSelected = { /* Update tab state */ },
                onAddClick = { showAddOptionsModal = true }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(top = 76.dp)
        ) {


                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF876232))
                    }
                } else if (books.isEmpty()) {
                    CookbooksScreen(onCreateClick = { showCreateDialog = true })
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 58.dp)
                    ) {
                        // Header Section
                        Text(
                            text = "Your Cookbooks",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Text(
                            text = "Organize and manage your favorite recipes with personalized cookbooks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 26.dp)
                        )

                        // Sorting and Filtering Row
                        // Sorting and Filtering Row
                        // Sorting and Filtering Row
                        // Sorting and Filtering Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Button for Title Sorting
                            Button(
                                onClick = {
                                    sortingMode = SortingMode.TITLE
                                    // Toggle the sort order for titles
                                    sortAscending = !sortAscending
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF000000
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(
                                        2.dp,
                                        Color(0xFF866232),
                                        RoundedCornerShape(8.dp)
                                    ) // Adds a white border
                            ) {
                                Text(
                                    text = if (sortAscending) "Sort A-Z" else "Sort Z-A",
                                    color = Color.White
                                )
                            }

                            // Button for Sorting by Recipes
                            Button(
                                onClick = {
                                    if (sortingMode == SortingMode.RECIPES) {
                                        sortRecipesAscending = !sortRecipesAscending
                                    } else {
                                        sortingMode = SortingMode.RECIPES
                                        sortRecipesAscending =
                                            false // default to descending (most recipes on top)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF000000
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(
                                        2.dp,
                                        Color(0xFF866232),
                                        RoundedCornerShape(8.dp)
                                    ) // Adds a white border
                            ) {
                                Text(
                                    text = if (sortingMode == SortingMode.RECIPES) {
                                        if (sortRecipesAscending) "Recipes Asc" else "Recipes Desc"
                                    } else {
                                        "Sort by Recipes"
                                    },
                                    color = Color.White
                                )
                            }


                        }



                        Spacer(modifier = Modifier.height(16.dp))

                        // List of Cookbooks
                        // List of Cookbooks
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 60.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sortedBooks) { book ->
                                BookCard(
                                    book = book,
                                    onClick = { navController.navigate("cookbookDetail/${book.id}") },
                                    onDelete = {
                                        coroutineScope.launch {
                                            val userId =
                                                AuthPreferences.getUserId(context) ?: return@launch
                                            book.id?.let {
                                                bookRepository.deleteBook(it) { success ->
                                                    if (success) {
                                                        books = books.filter { it.id != book.id }
                                                        Toast.makeText(
                                                            context,
                                                            "Cookbook deleted!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to delete cookbook.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onEdit = { updatedBook ->
                                        coroutineScope.launch {
                                            // Ensure the book has a valid id
                                            val bookId = updatedBook.id ?: return@launch
                                            bookRepository.updateBook(
                                                bookId,
                                                updatedBook.toDTO()
                                            ) { result ->
                                                if (result != null) {
                                                    books =
                                                        books.map { if (it.id == result.id) result else it }
                                                    Toast.makeText(
                                                        context,
                                                        "Cookbook updated!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to update cookbook.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }

                                )
                                Divider(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }


                if (showCreateDialog) {
                    CreateCookbookDialog(
                        onDismiss = { showCreateDialog = false },
                        onCreate = { title, description, selectedColor, isPublic ->
                            coroutineScope.launch {
                                val userId = AuthPreferences.getUserId(context) ?: return@launch
                                val newBook = BookDTO(
                                    title = title,
                                    description = description,
                                    authorId = userId,
                                    recipeIds = emptyList(),
                                    color = selectedColor,
                                    isPublic = isPublic
                                    // You'll need to add color to your BookDTO (see your backend DTO updates)
                                )
                                // Make sure to set the color on the newBook (or update your repository method accordingly)
                                newBook.color = selectedColor
                                bookRepository.createBook(newBook) { createdBook ->
                                    if (createdBook != null) {
                                        books = books + createdBook
                                        Toast.makeText(
                                            context,
                                            "Cookbook created!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to create cookbook.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    showCreateDialog = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(book: Book, onClick: () -> Unit, onDelete: () -> Unit,  onEdit: (Book) -> Unit ) {
    var showDropdown by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val safeColorString = book.color ?: "#866232"
    var editTitle by remember { mutableStateOf(book.title) }
    var editDescription by remember { mutableStateOf(book.description) }
    var editColor by remember { mutableStateOf(book.color ?: "#866232") }

    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    val bookColor = try {
        Color(android.graphics.Color.parseColor(safeColorString))
    } catch (e: Exception) {
        Color(0xFF866232)
    }
    val currentUserId = AuthPreferences.getUserId(context) // Get logged-in user ID
    val isOwner = currentUserId == book.authorId
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
            .background(bookColor) // Apply the parsed color
            .shadow(8.dp, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(29.dp)
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Book Title
            // ✅ Title Label


            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { showDropdown = true }
                    )

            ) {
                // Book Spine (Left Strip)
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF876232))
                        .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Book Cover
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_with_hat),
                        contentDescription = "Book Cover",
                        tint = Color(0xD0FFFFFF),
                        modifier = Modifier.size(46.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                // Book Details
                Column {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1F1F1F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${book.recipeIds.size} recipes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.8f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (book.isPublic) android.R.drawable.ic_menu_view else android.R.drawable.ic_secure
                            ),
                            contentDescription = if (book.isPublic) "Public" else "Private",
                            tint = if (book.isPublic) Color.Green else Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (book.isPublic) "Public" else "Private",
                            color = if (book.isPublic) Color.Green else Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

// Dropdown Menu
        // Dropdown Menu with Edit and Delete options
        if (isOwner) {
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            DropdownMenuItem(
                text = { Text("Edit", color = Color.Blue) },
                onClick = {
                    // Pre-populate edit fields
                    editTitle = book.title
                    editDescription = book.description
                    editColor = book.color ?: "#866232"
                    showEditDialog = true
                    showDropdown = false
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = Color.Red) },
                onClick = {
                    showConfirmationDialog = true
                    showDropdown = false
                }
            )
        }
        }
    }

    // Show Confirmation Dialog
    if (showConfirmationDialog) {
        ConfirmationDialog(
            title = "Delete Cookbook?",
            message = "Are you sure you want to delete this cookbook? This action cannot be undone.",
            onConfirm = {
                onDelete()
                showConfirmationDialog = false
            },
            onDismiss = { showConfirmationDialog = false }
        )
    }
    // Show Edit Dialog when requested
    if (showEditDialog) {
        EditCookbookDialog(
            initialTitle = editTitle,
            initialDescription = editDescription,
            initialColor = editColor,
            initialIsPublic = book.isPublic, // ✅ Pass public status
            onDismiss = { showEditDialog = false },
            onEdit = { newTitle, newDescription, newColor, newIsPublic -> // ✅ Accept isPublic
                onEdit(book.copy(title = newTitle, description = newDescription, color = newColor, isPublic = newIsPublic))
                showEditDialog = false
            }
        )
    }
}

@Composable
fun CookbooksTopBar(onSettingsClick: () -> Unit ,  navController: NavController) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 8.dp), // Reduced vertical padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Empty spacer to balance the layout
            Spacer(modifier = Modifier.weight(1f))

            // Centered logo and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(16f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_loogo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF886232)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LeGourmand",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }

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
                .background(Color.Gray.copy(alpha = 0.3f))
        )
    }
}


@Composable
fun EmptyCookbooksState(onCreateClick: () -> Unit) {
    // You can wrap everything in a Box to set the background color
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Replace R.drawable.ic_book_with_hat with your actual icon
            Icon(
                painter = painterResource(R.drawable.ic_book_with_hat),
                contentDescription = null,
                tint = Color(0xFF886232),
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Create Your First Cookbook",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Organize recipes and cook together with friends and family.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF866232)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Create cookbook", color = Color.White)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCookbookDialog(onDismiss: () -> Unit, onCreate: (String, String, String, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#866232") } // Default color
    var isPublic by remember { mutableStateOf(false) } // ✅ New state for public/private setting

    // A list of enhanced color options.
    val colorOptions = listOf(
        "#FF5733", "#33FF57", "#5733FF", "#FFFF33",
        "#FF33A1", "#33FFF5", "#A133FF", "#FFC300",
        "#4CAF50", "#2196F3", "#F44336", "#9C27B0",
        "#E91E63", "#00BCD4", "#8BC34A", "#FF9800"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF333333)  // Dark background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Create New Cookbook",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color(0xFF866232),
                        focusedBorderColor = Color(0xFF866232),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF866232),
                        unfocusedLabelColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                        keyboardType = KeyboardType.Text // Adjusts the type of keyboard
                    ),
                    maxLines = 3,
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color(0xFF866232),
                        focusedBorderColor = Color(0xFF866232),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF866232),
                        unfocusedLabelColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Enhanced Color Picker with LazyRow
                Text("Choose a color", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colorOptions) { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .border(
                                    width = if (selectedColor == colorHex) 3.dp else 1.dp,
                                    color = if (selectedColor == colorHex) Color.White else Color.LightGray,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Display the selected color clearly.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Selected Color:", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(android.graphics.Color.parseColor(selectedColor)))
                            .border(1.dp, Color.White, shape = RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedColor, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPublic = !isPublic }
                ) {
                    Checkbox(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF866232))
                    )
                    Text("Make this cookbook public", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        if (title.isNotBlank()) {
                            onCreate(title, description, selectedColor, isPublic) // ✅ Pass isPublic
                        }
                    }) {
                        Text("Create", color = Color(0xFF866232))
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbooksScreen(
    onCreateClick: () -> Unit
) {
    Scaffold(
        // Make the background color black, if desired
        containerColor = Color.Black,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cookbooks",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                // Use a black background to match the screenshot
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                ),
                actions = {
                    // "Create" button on the right side of the top bar
                    Button(
                        onClick = onCreateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF876232)),
                        shape = RoundedCornerShape(10.dp)  // Adjust the corner radius as desired
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Create", color = Color.White)
                    }

                }
            )
        },

        // The body content of the screen
        content = { padding ->
            // You can put your EmptyCookbooksState composable here, for example:
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp)
            ) {
                EmptyCookbooksState(onCreateClick = onCreateClick)
            }
        }
    )
}
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2E2E2E) // Dark background for contrast
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_dialog_alert),
                    contentDescription = "Warning Icon",
                    tint = Color.Yellow,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                }
            }
        }
    }
}
enum class SortingMode {
    TITLE, RECIPES
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCookbookDialog(
    initialTitle: String,
    initialDescription: String,
    initialColor: String,
    initialIsPublic: Boolean,
    onDismiss: () -> Unit,
    onEdit: (String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var isPublic by remember { mutableStateOf(initialIsPublic) }
    // A list of enhanced color options.
    val colorOptions = listOf(
        "#FF5733", "#33FF57", "#5733FF", "#FFFF33",
        "#FF33A1", "#33FFF5", "#A133FF", "#FFC300",
        "#4CAF50", "#2196F3", "#F44336", "#9C27B0",
        "#E91E63", "#00BCD4", "#8BC34A", "#FF9800"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF333333)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Edit Cookbook",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color(0xFF866232),
                        focusedBorderColor = Color(0xFF866232),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF866232),
                        unfocusedLabelColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                        keyboardType = KeyboardType.Text // Adjusts the type of keyboard
                    ),
                    maxLines = 3,
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color(0xFF866232),
                        focusedBorderColor = Color(0xFF866232),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF866232),
                        unfocusedLabelColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Choose a color", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colorOptions) { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .border(
                                    width = if (selectedColor == colorHex) 3.dp else 1.dp,
                                    color = if (selectedColor == colorHex) Color.White else Color.LightGray,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPublic = !isPublic }
                ) {
                    Checkbox(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF866232))
                    )
                    Text(
                        text = if (isPublic) "Public" else "Private",
                        color = if (isPublic) Color.Green else Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Selected Color:", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(android.graphics.Color.parseColor(selectedColor)))
                            .border(1.dp, Color.White, shape = RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedColor, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        if (title.isNotBlank()) {
                            onEdit(title, description, selectedColor, isPublic)
                        }
                    }) {
                        Text("Save", color = Color(0xFF866232))
                    }
                }
            }
        }
    }
}
