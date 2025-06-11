package com.example.recipeapp.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // Make sure you have this import
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.network.AppwriteClient
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.AuthService
import com.example.recipeapp.utils.Book
import com.example.recipeapp.utils.BookRepository
import com.example.recipeapp.utils.Note

import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO
import com.example.recipeapp.viewmodels.ProfileViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.threeten.bp.LocalDate
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorDetailScreen(
    authorId: Long,
    navController: NavController,
    recipes: MutableList<Recipe>
) {
    val context = LocalContext.current

    // States
    var author by remember { mutableStateOf<UserDTO?>(null) }
    var allRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // For search and filter
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    // Example difficulties to choose from
    val difficulties = listOf("Easy", "Medium", "Hard")

    // Services
    val authService = ApiClient.getAuthService(context)
    val recipeRepository = RecipeRepository(context)

    var showAddOptionsModal by remember { mutableStateOf(false) }
    var isSheetVisible by remember { mutableStateOf(false) }

    // Manage planned recipes and notes
    val plannedRecipes = remember { mutableStateMapOf<LocalDate, SnapshotStateList<Recipe>>() }
    val notes = remember { mutableStateMapOf<LocalDate, SnapshotStateList<Note>>() }
    var user by remember { mutableStateOf<UserDTO?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }

    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // Google & Firebase
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("832072464275-84unkql8c2h7qa5u27b083k1mb2f6671.apps.googleusercontent.com")
        .requestEmail()
        .build()
    val googleSignInClient: GoogleSignInClient =
        GoogleSignIn.getClient(context, googleSignInOptions)
    val firebaseAuth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()
    val bookRepository = remember { BookRepository(context) }
    var allBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    var searchCookbookQuery by remember { mutableStateOf("") }

// Derived filtered cookbooks list
    val filteredCookbooks = allBooks.filter { book ->
        book.title.contains(searchCookbookQuery, ignoreCase = true)
    }
    LaunchedEffect(author) {
        Log.d("ProfileHeader", "Author badges: ${author?.badges}")
    }
    // Fetch subscription info
    LaunchedEffect(Unit) {
        authService.getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { userDto ->
                        when (userDto.subscriptionType?.uppercase() ?: "FREE") {
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
                    Log.e(
                        "ProfileScreen",
                        "Failed to fetch profile: ${response.code()} ${response.message()}"
                    )
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e(
                    "ProfileScreen",
                    "Error fetching profile: ${t.localizedMessage ?: "Unknown error"}"
                )
            }
        })
    }

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

    // Function to refresh author data
    fun refreshAuthor() {
        authService.getUserById(authorId).enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { fetchedUser ->
                        Log.d("ProfileHeader", "Fetched user: $fetchedUser")
                        author = fetchedUser
                    }
                } else {
                    Log.e("AuthorDetailScreen", "Error: ${response.code()} ${response.message()}")
                }
            }
            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("AuthorDetailScreen", "Error refreshing author: ${t.localizedMessage}")
            }
        })
    }
    // 1) Fetch user & recipes
    LaunchedEffect(authorId) {
        // Fetch user info
        authService.getUserById(authorId).enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    author = response.body()
                } else {
                    Log.e("AuthorDetailScreen", "Error: ${response.code()} ${response.message()}")
                }
            }
            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("AuthorDetailScreen", "Error fetching author: ${t.localizedMessage}")
            }
        })

        // Fetch recipes by this author
        recipeRepository.getRecipesByAuthorId(authorId) { fetchedRecipes ->
            val currentUserId = AuthPreferences.getUserId(context)
            allRecipes = if (currentUserId != authorId) {
                fetchedRecipes?.filter { it.isPublic } ?: emptyList()
            } else {
                fetchedRecipes ?: emptyList()
            }
            isLoading = false
        }

    }
    LaunchedEffect(authorId) {
        val currentUserId = AuthPreferences.getUserId(context)

        bookRepository.getBooksByUserId(authorId) { fetchedBooks ->
            Log.d("DEBUG_BOOKS", "Fetched books: $fetchedBooks") // Debugging log

            if (fetchedBooks != null) {
                allBooks = if (currentUserId != authorId) {
                    // Only show public books for other users
                    fetchedBooks.filter { book ->
                        Log.d("DEBUG_BOOKS", "Checking book: ${book.title} - isPublic: ${book.isPublic}")
                        book.isPublic
                    }
                } else {
                    // Show all books if the user is the owner
                    fetchedBooks
                }
            } else {
                allBooks = emptyList()
            }

            Log.d("DEBUG_BOOKS", "Final books to display: $allBooks") // Verify final book list
        }
    }

    // 2) Derive the final filtered list from the states
    val filteredRecipes = allRecipes.filter { recipe ->
        // Filter by search query in the recipe title (case-insensitive)
        val matchesSearch = recipe.title.contains(searchQuery, ignoreCase = true)

        // Filter by selected difficulty if set
        val matchesDifficulty = selectedDifficulty?.let {
            recipe.difficulty.equals(it, ignoreCase = true)
        } ?: true // If no difficulty is selected, always true

        matchesSearch && matchesDifficulty
    }


    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_loogo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF886232)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LeGourmand", fontSize = 20.sp, color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        androidx.compose.material3.Text(
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
                        containerColor = Color(0xFF211312),
                        titleContentColor = Color.White
                    )
                )
                Divider(
                    color = Color(0xFF474545),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedTab = "profile",
                onTabSelected = { },
                onAddClick = { }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF876232))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF1D1C1C))
            ) {
                // Profile Header
                ProfileHeaderSection(
                    author = author,
                    onAuthorUpdated = { updatedAuthor -> author = updatedAuthor },
                    refreshAuthor = { }
                )

                // Tab Section: Public Recipes | Public Cookbooks
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF211312), // Background color
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF866232) // Change this to your desired color
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        modifier = Modifier.weight(1f), // Ensure equal space for both tabs
                        text = { Text("Public Recipes", textAlign = TextAlign.Center) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        modifier = Modifier.weight(1f), // Ensure equal space for both tabs
                        text = { Text("Public Cookbooks", textAlign = TextAlign.Center) }
                    )
                }


                when (selectedTabIndex) {
                    0 -> { // **Public Recipes Section**
                        PublicRecipesHeadingAndSearch(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            onFilterClick = { showFilterDialog = true }
                        )

                        if (showFilterDialog) {
                            DifficultyFilterDialog(
                                difficulties = difficulties,
                                selectedDifficulty = selectedDifficulty,
                                onDismiss = { showFilterDialog = false },
                                onDifficultySelected = {
                                    selectedDifficulty = it
                                    showFilterDialog = false
                                },
                                onClearFilters = {
                                    selectedDifficulty = null
                                    showFilterDialog = false
                                }
                            )
                        }

                        if (filteredRecipes.isNotEmpty()) {
                            AuthorRecipesGrid2Columns(
                                recipes = filteredRecipes,
                                navController = navController
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No recipes found.", color = Color.White)
                            }
                        }
                    }

                    1 -> { // **Public Cookbooks Section**
                        PublicCookbooksHeadingAndSearch(
                            searchQuery = searchCookbookQuery,
                            onSearchChange = { searchCookbookQuery = it }
                        )

                        if (filteredCookbooks.isNotEmpty()) {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredCookbooks) { book ->
                                    BookCard(
                                        book = book,
                                        onClick = { navController.navigate("cookbookDetail/${book.id}") },
                                        onDelete = {},
                                        onEdit = {}
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No public cookbooks found.", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

}

/**
 * A profile header with an avatar placeholder, username, stats, and a "Follow" button.
 */
@Composable
fun ProfileHeaderSection(
    author: UserDTO?,
    onAuthorUpdated: (UserDTO) -> Unit,
    refreshAuthor: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId = AuthPreferences.getUserId(context) ?: 0L
    val isOwnProfile = (author?.id == currentUserId)
    var isFollowing by rememberSaveable { mutableStateOf(false) }
    val authService = ApiClient.getAuthService(context)


    LaunchedEffect(Unit) {
        AppwriteClient.initClient(context)  // Ensure Appwrite is initialized before using it
    }
    // New state: selected avatar URI (if any)

    // State to hold the selected avatar URI.
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    // Function to upload the avatar.
    fun uploadAvatar(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Get the ProfileViewModel from the ViewModelStoreOwner (typically your Activity)
        val viewModel = ViewModelProvider(context as ViewModelStoreOwner).get(ProfileViewModel::class.java)

        viewModel.uploadAndModerateAvatarImage(context, uri) { uploadedUrl ->
            if (uploadedUrl != null) {
                onSuccess(uploadedUrl) // Successfully uploaded and moderated; return URL.
            } else {
                onError("Failed to upload and moderate image to Appwrite")
            }
        }
    }


    // Launcher to pick an image from storage.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri
            uploadAvatar(context, uri,
                onSuccess = { newAvatarUrl ->
                    refreshAuthor() // Refresh profile to get updated avatar
                },
                onError = { errorMsg ->
                    Log.e("ProfileHeader", "Upload failed: $errorMsg")
                }
            )
        }
    }

    // Helper function to check follow status
    fun checkFollowStatus() {
        if (author != null && !isOwnProfile) {
            authService.isFollowingUser(author.id!!).enqueue(object : Callback<Boolean> {
                override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                    if (response.isSuccessful) {
                        isFollowing = response.body() ?: false
                    }
                }
                override fun onFailure(call: Call<Boolean>, t: Throwable) {
                    Log.e("Follow", "Error checking follow status: ${t.localizedMessage}")
                }
            })
        }
    }
    fun deleteAvatar() {
        authService.deleteAvatar().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    avatarUri = null
                    refreshAuthor() // Refresh to load the updated profile
                } else {
                    Log.e("ProfileHeader", "Failed to delete avatar: ${response.code()} ${response.message()}")
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("ProfileHeader", "Error deleting avatar: ${t.localizedMessage}")
            }
        })
    }


    // Initial check when author is loaded
    LaunchedEffect(author) {
        checkFollowStatus()
    }

    // Observe lifecycle events (e.g. when the screen resumes)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, author) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkFollowStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        // Avatar placeholder with initials
        // Avatar: When tapped (if own profile), launches image picker.
        Surface(
            shape = CircleShape,
            color = Color.Gray,
            modifier = Modifier
                .size(64.dp)
                .clickable {
                    if (isOwnProfile) {
                        imagePickerLauncher.launch("image/*")
                    }
                }
        ) {
            // LaunchedEffect will run when avatarUri changes.
            LaunchedEffect(avatarUri) {
                Log.d("ProfilePic", "New avatarUri chosen: $avatarUri")
                // You can add additional actions here if needed.
            }
            LaunchedEffect(author) {
                val backendImageUri = author?.imageUri
                if (!backendImageUri.isNullOrBlank()) {
                    avatarUri = Uri.parse(backendImageUri)
                    Log.d("ProfilePic", "Updated avatarUri from backend: $avatarUri")
                } else {
                    Log.d("ProfilePic", "Backend imageUri is null or blank")
                }
            }


            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // If a new image was picked locally, display it.
                    avatarUri != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(model = avatarUri),
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Otherwise, if the user has an avatar URL from the backend, load it.
                    author?.imageUri?.isNotEmpty() == true -> {
                        Image(
                            painter = rememberAsyncImagePainter(model = author?.imageUri),
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Fallback: show initials from the username.
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = author?.username?.take(2)?.uppercase() ?: "NN",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
// Overlay an "X" button to delete the avatar (only show if it's the user's own profile and there’s an image)
                if (isOwnProfile && (avatarUri != null || (author?.imageUri?.isNotEmpty() == true))) {
                    IconButton(
                        onClick = { deleteAvatar() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(30.dp)

                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete Avatar",
                            tint = Color(0xFFFFFFFF),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }


        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = author?.username ?: "Unknown user",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                // NEW: Display badges if available

                Spacer(modifier = Modifier.width(8.dp)) // Adjust spacing as needed

                val subType = author?.subscriptionType?.uppercase()
                if (subType == "PLUS" || subType == "PRO") {
                    SubscriptionBadge(subscriptionType = subType)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Dynamic follower and following counts
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${author?.followerCount ?: 0}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Followers", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${author?.followingCount ?: 0}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Following", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        // Display user badges in profile

        // Show follow/unfollow button if not own profile
        if (!isOwnProfile) {
            Button(
                onClick = {
                    if (isFollowing) {
                        // Unfollow API call
                        authService.unfollowUser(author?.id ?: 0L).enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                if (response.isSuccessful) {
                                    isFollowing = false
                                    author?.let {
                                        onAuthorUpdated(
                                            it.copy(followerCount = (it.followerCount ?: 0) - 1)
                                        )
                                    }
                                    refreshAuthor() // re-fetch updated data
                                } else {
                                    Log.e("Follow", "Unfollow error: ${response.code()} ${response.message()}")
                                }
                            }
                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                Log.e("Follow", "Network error: ${t.localizedMessage}")
                            }
                        })
                    } else {
                        // Follow API call
                        authService.followUser(author?.id ?: 0L).enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                if (response.isSuccessful) {
                                    isFollowing = true
                                    author?.let {
                                        onAuthorUpdated(
                                            it.copy(followerCount = (it.followerCount ?: 0) + 1)
                                        )
                                    }
                                    refreshAuthor() // re-fetch updated data
                                } else {
                                    Log.e("Follow", "Follow error: ${response.code()} ${response.message()}")
                                }
                            }
                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                Log.e("Follow", "Network error: ${t.localizedMessage}")
                            }
                        })
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) Color.Gray else Color(0xFF876232)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isFollowing) "Unfollow" else "Follow",
                    color = Color.White
                )
            }
        }
    }

    if (!author?.badges.isNullOrEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 8.dp).padding(start = 16.dp)
        ) {
            CompactBadgeDisplay(author!!.badges) // ✅ Call it once!
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
}

/**
 * Shows recipes in a 2-column grid, each item is a Card with an image + title.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthorRecipesGrid2Columns(
    recipes: List<Recipe>,
    navController: NavController
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recipes) { recipe ->
            // Each recipe card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF886232)),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { navController.navigate("recipeDetail/${recipe.id}") }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // If the recipe has an imageUri, load it with coil, else placeholder
                    if (!recipe.imageUri.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = Uri.parse(recipe.imageUri)),
                            contentDescription = recipe.title,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // If no image, show some placeholder icon
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_loogo),
                                contentDescription = "Placeholder",
                                tint = Color(0xFF2E2E2E),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Recipe title
                    Text(
                        text = recipe.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicRecipesHeadingAndSearch(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(18.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically // Align contents vertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text(text = "Search...", color = Color.Gray) },
            modifier = Modifier
                .weight(1f)
                .height(68.dp), // Make the height consistent
            textStyle = TextStyle(color = Color.White),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                keyboardType = KeyboardType.Text // Adjusts the type of keyboard
            ),
            shape = RoundedCornerShape(10.dp), // Less rounded corners
            colors = outlinedTextFieldColors(
                focusedLabelColor = Color(0xFF00897B),
                unfocusedLabelColor = Color.Gray,
                focusedBorderColor = Color(0xFF00897B),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.White
            )
        )

        Button(
            onClick = onFilterClick,
            shape = RoundedCornerShape(10.dp), // Match textfield corners
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x6A00897B)),
            modifier = Modifier.height(48.dp) // Same height as textfield
        ) {
            Text(text = "Filters", color = Color.White)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}


/**
 * Shows recipes in a 2-column grid, each item is a Card with an image + title.
 */
/**
 * A dialog that shows difficulty options and allows clearing filters.
 */
@Composable
fun DifficultyFilterDialog(
    difficulties: List<String>,
    selectedDifficulty: String?,
    onDismiss: () -> Unit,
    onDifficultySelected: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        text = {
            Column {
                Text(
                    text = "Select Difficulty",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))

                difficulties.forEach { diff ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDifficultySelected(diff) }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = (diff == selectedDifficulty),
                            onClick = { onDifficultySelected(diff) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = diff, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Clear filters button
                Button(
                    onClick = { onClearFilters() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Clear Filters", color = Color.Black)
                }

                Spacer(modifier = Modifier.height(4.dp))
                // Cancel button
                OutlinedButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Close", color = Color.Black)
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(12.dp)
    )

}
@Composable
fun CompactBadgeDisplay(badges: Map<String, Int>) {
    val rankIcons = mapOf(
        "master chef" to "🥇",  // First Place
        "elite cook" to "🥈",  // Second Place
        "challenger star" to "🥉"  // Third Place
    )

    var selectedBadge by remember { mutableStateOf<String?>(null) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        badges.forEach { (badgeName, count) ->
            val normalizedBadge = badgeName.lowercase()
            val icon = rankIcons[normalizedBadge] ?: "🏆"
            BadgeChip(count, icon, badgeName) {
                selectedBadge = badgeName
            }
        }
    }

    // Show pop-up dialog when a badge is clicked
    selectedBadge?.let { badge ->
        BadgeInfoDialog(
            badgeName = badge,
            onDismiss = { selectedBadge = null }
        )
    }
}
@Composable
fun BadgeChip(count: Int, icon: String, badgeName: String, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()

    // **Glow Animation**
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow Alpha"
    )

    // **Floating Up & Down Animation**
    val verticalShift by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Floating Animation"
    )

    // **Border Glow Animation**
    // **Fix: Make the border always visible to avoid flickering**
    val borderGlow by infiniteTransition.animateColor(
        initialValue = Color.White.copy(alpha = 0.5f),  // Start with semi-glow
        targetValue = Color.White.copy(alpha = glowAlpha),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Border Glow Animation"
    )


    Box(
        modifier = Modifier
            .padding(6.dp)
            .clickable { onClick() }
            .offset(y = verticalShift.dp) // Floating Effect
            .graphicsLayer {
                shadowElevation = 10f
                rotationZ = verticalShift // Slight rotation effect
            }
    ) {
        Surface(
            shape = RoundedCornerShape(30),
            border = BorderStroke(4.dp, borderGlow), // **Glowing Border**
            modifier = Modifier
                .shadow(12.dp, shape = RoundedCornerShape(30)) // **Extra Glow**
                .background(getBadgeColor(badgeName)) // **Gradient Background**
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "$icon x $count",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}


@Composable
fun BadgeInfoDialog(badgeName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        },
        title = {
            Text(
                text = badgeName.replaceFirstChar { it.uppercase() },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "🏆 This represents your achievement: $badgeName",
                color = Color.White
            )
        },
        containerColor = Color(0xFF211312),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun getBadgeColor(badgeName: String): Brush {
    val infiniteTransition = rememberInfiniteTransition()

    val colorShift by infiniteTransition.animateColor(
        initialValue = Color(0xFFFFD700), // Gold
        targetValue = Color(0xFFFFA500), // Orange
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Badge Gradient Shift"
    )

    return when (badgeName.lowercase()) {
        "master chef" -> Brush.horizontalGradient(
            colors = listOf(colorShift, Color(0xFFFFD700))
        )
        "elite cook" -> Brush.horizontalGradient(
            colors = listOf(Color.LightGray, Color.White)
        )
        "challenger star" -> Brush.horizontalGradient(
            colors = listOf(Color(0xFFCD7F32), Color(0xFF8B4513))
        )
        else -> Brush.horizontalGradient(
            colors = listOf(Color(0xFF876232), Color(0xFF3E2723))
        )
    }
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookCardzzz(
    book: Book,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (Book) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentUserId = AuthPreferences.getUserId(context)
    val isOwner = currentUserId == book.authorId

    val safeColorString = book.color ?: "#866232"
    val bookColor = try {
        Color(android.graphics.Color.parseColor(safeColorString))
    } catch (e: Exception) {
        Color(0xFF866232) // Default color
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bookColor)
            .shadow(8.dp, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ Title Label

            Spacer(modifier = Modifier.height(12.dp))
            // ✅ Book Title
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ✅ Description Label



            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Recipe Count & Privacy Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 6.dp), // 🔹 Add left padding & spacing
                horizontalArrangement = Arrangement.Start, // 🔹 Move text to the right
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_with_hat),
                    contentDescription = "Book Cover",
                    tint = Color(0xD0FFFFFF),
                    modifier = Modifier.size(46.dp)
                )
                Text(
                    text = "${book.recipeIds.size} recipes",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp) // 🔹 Move text further right
                )
            }
        }

        // ✅ Dropdown menu (only for the owner)
        if (isOwner) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = Color.Blue) },
                        onClick = {
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
    }

    // ✅ Show Confirmation Dialog
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

    // ✅ Show Edit Dialog when requested
    if (showEditDialog) {
        EditCookbookDialog(
            initialTitle = book.title,
            initialDescription = book.description,
            initialColor = book.color ?: "#866232",
            initialIsPublic = book.isPublic, // ✅ Ensure `isPublic` is included
            onDismiss = { showEditDialog = false },
            onEdit = { newTitle, newDescription, newColor, newIsPublic ->
                onEdit(book.copy(title = newTitle, description = newDescription, color = newColor, isPublic = newIsPublic))
                showEditDialog = false
            }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicCookbooksHeadingAndSearch(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(18.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = { Text(text = "Search Cookbooks...", color = Color.Gray) },
            modifier = Modifier
                .weight(1f)
                .height(68.dp),
            textStyle = TextStyle(color = Color.White),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            shape = RoundedCornerShape(10.dp),
            colors = outlinedTextFieldColors(
                focusedLabelColor = Color(0xFF00897B),
                unfocusedLabelColor = Color.Gray,
                focusedBorderColor = Color(0xFF00897B),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.White
            )
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}
