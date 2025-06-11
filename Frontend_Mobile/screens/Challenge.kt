// File: CookingChallengesScreen.kt
package com.example.recipeapp.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.Book
import com.example.recipeapp.utils.BookRepository
import com.example.recipeapp.utils.ChallengeDTO
import com.example.recipeapp.utils.ChallengeRepository
import com.example.recipeapp.utils.ChallengesViewModelFactory
import com.example.recipeapp.utils.ChallengeApi
import com.example.recipeapp.utils.FeaturedWinner
import com.example.recipeapp.utils.GlobalLeaderboardEntry
import com.example.recipeapp.utils.Note
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO
import com.example.recipeapp.viewmodel.ChallengesViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingChallengesScreen(
    onChallengeClick: (ChallengeDTO) -> Unit = {},
    onSubmitChallenge: () -> Unit = {},
    navController: NavController,
    recipes: MutableList<Recipe>
) {
    val context = LocalContext.current

    // Create ChallengeApi using your Retrofit instance
    val challengeApi: ChallengeApi = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)
    val repository = ChallengeRepository(challengeApi)
    val factory = ChallengesViewModelFactory(repository)
    val viewModel: ChallengesViewModel = viewModel(factory = factory)


    val challengeRepository = ChallengeRepository(challengeApi)

    // State for challenge details, loading and error messages.
    var challengeDetail by remember { mutableStateOf<ChallengeDTO?>(null) }

    // State for submitted recipes.
    var submittedRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }

    // Modal bottom sheet state for recipe submission.

    var currentUserEmail by remember { mutableStateOf("") }
    val authService = ApiClient.getAuthService(context)


// Fetch user details from the API
    LaunchedEffect(Unit) {
        authService.getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        currentUserEmail = user.email ?: ""
                        Log.d("ChallengeDetailScreen", "Fetched User Email: $currentUserEmail")
                    }
                } else {
                    Log.e("ChallengeDetailScreen", "Failed to fetch user email: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("ChallengeDetailScreen", "Error fetching user email: ${t.localizedMessage ?: "Unknown error"}")
            }
        })
    }

    // Trigger a fresh API call to update the user's role.
    LaunchedEffect(Unit) {
        ApiClient.getAuthService(context).getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.role?.let { newRole ->
                        AuthPreferences.saveUserRole(context, newRole)
                        Log.d("USER_ROLE_DEBUG", "Updated role: $newRole")
                    }
                } else {
                    Log.e("USER_ROLE_DEBUG", "Failed to fetch profile: ${response.code()} ${response.message()}")
                }
            }
            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("USER_ROLE_DEBUG", "Error fetching profile: ${t.localizedMessage}")
            }
        })
    }

    // Refresh challenges whenever the screen resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchChallenges()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val challenges by viewModel.challenges.collectAsState()

    // Retrieve the current user's role (default to "user" if not found)
    val userRole = AuthPreferences.getUserRole(context) ?: "user"

    // Filter active challenges (deadline in the future)
    LaunchedEffect(challenges) {
        Log.d("ActiveChallenges", "✅ Active Challenges: ${challenges.map { it.title to it.featured }}")
    }

// Filter only active challenges (deadline not passed)
    val activeChallenges = remember {
        derivedStateOf {
            challenges.filter { challenge ->
                try {
                    val deadlineDate = LocalDate.parse(challenge.deadline)
                    !LocalDate.now().isAfter(deadlineDate)
                } catch (e: Exception) {
                    Log.e("ActiveChallenges", "❌ Date parsing error: ${challenge.deadline}")
                    false
                }
            }
        }
    }.value

    // Fetch Featured Challenge Titles from API
    val featuredChallengeTitles = remember { mutableStateListOf<String>() }

// API Call to get featured challenge titles
    LaunchedEffect(Unit) {
        val api = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)
        api.getFeaturedChallengeNames().enqueue(object : Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                if (response.isSuccessful) {
                    featuredChallengeTitles.clear()
                    response.body()?.let {
                        featuredChallengeTitles.addAll(it)
                        Log.d("FeaturedChallenges", "✅ Fetched Featured Challenge Titles: ${featuredChallengeTitles.joinToString()}")
                    }
                } else {
                    Log.e("FeaturedChallenges", "❌ Failed to fetch featured challenge names: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                Log.e("FeaturedChallenges", "🚨 API Call Failed: ${t.localizedMessage}")
            }
        })
    }

    val matchingFeaturedChallenges = activeChallenges.filter { it.featured && featuredChallengeTitles.contains(it.title) }

    Log.d("FeaturedChallenges", "✅ Matching Featured Challenges: $matchingFeaturedChallenges")

    val featuredChallenge = matchingFeaturedChallenges.firstOrNull()

    if (featuredChallenge != null) {
        Log.d("FeaturedChallenges", "✅ Selected Featured Challenge: ${featuredChallenge.title}")
    } else {
        Log.e("FeaturedChallenges", "❌ No Featured Challenge Selected!")
    }


// Subscription state variables
    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }

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
                    Log.e("USER_ROLE_DEBUG", "Failed to fetch profile: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("USER_ROLE_DEBUG", "Error fetching profile: ${t.localizedMessage ?: "Unknown error"}")
            }
        })
    }

// 🔥 Show FloatingActionButton only for ADMIN or PRO users

    // Weekly challenges should EXCLUDE any challenge that was meant to be featured
    val weeklyChallenges = activeChallenges.filter { !featuredChallengeTitles.contains(it.title) }
    var username by remember { mutableStateOf("User") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var favoritesCount by remember { mutableStateOf(0) }
    var isFavoritesLoading by remember { mutableStateOf(false) }
    var favoritesError by remember { mutableStateOf<String?>(null) }

    val recipeRepository = remember { RecipeRepository(context) }
    val bookRepository = remember { BookRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }

    var showAddOptionsModal by remember { mutableStateOf(false) }
    var isSheetVisible by remember { mutableStateOf(false) }

    // Manage planned recipes and notes
    val plannedRecipes = remember { mutableStateMapOf<org.threeten.bp.LocalDate, SnapshotStateList<Recipe>>() }
    val notes = remember { mutableStateMapOf<org.threeten.bp.LocalDate, SnapshotStateList<Note>>() }
    var user by remember { mutableStateOf<UserDTO?>(null) }


    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // Google & Firebase
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("832072464275-84unkql8c2h7qa5u27b083k1mb2f6671.apps.googleusercontent.com")
        .requestEmail()
        .build()
    val googleSignInClient: GoogleSignInClient =
        GoogleSignIn.getClient(context, googleSignInOptions)
    val firebaseAuth = FirebaseAuth.getInstance()
    val sheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    if (errorMessage != null || favoritesError != null) {
        // Show error page
        ErrorPageDarkz(
            message = errorMessage ?: favoritesError!!,
            navController = navController,
            onRetry = {
                // Retry logic
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
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Add the logo to the left of the title
                                androidx.compose.material3.Icon(
                                    painter = painterResource(id = R.drawable.ic_loogo),
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(48.dp), // Adjust size as needed
                                    tint = Color(0xFF886232)
                                )
                                Spacer(modifier = Modifier.width(8.dp)) // Add some spacing between the logo and title
                                androidx.compose.material3.Text("LeGourmand", fontSize = 20.sp, color = Color.White)
                            }
                        },
                        navigationIcon = {
                            androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                                androidx.compose.material3.Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        actions = {
                            // Add the settings icon
                            androidx.compose.material3.Text(
                                text = "Upgrade",
                                modifier = Modifier
                                    .clickable { navController.navigate("premium") }
                                    .padding(13.dp),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Cursive,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color(0xFF211312),
                            titleContentColor = Color.White
                        )
                    )
                    // Add a Divider as a bottom border
                    androidx.compose.material3.Divider(
                        color = Color(0xFF474545), // Choose the color of the border
                        thickness = 1.dp, // Set the thickness of the border
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            bottomBar = {
                // Still use your custom bottom bar but with dark theme
                BottomNavigationBar(
                    navController = navController,
                    selectedTab = "home",
                    onTabSelected = { /* no-op */ },
                    onAddClick = { showAddOptionsModal = true }
                )
            },
            floatingActionButton = {
                if (userRole.equals("ADMIN", ignoreCase = true)) {
                    FloatingActionButton(onClick = {
                        Log.d("USER_ROLE_DEBUG", "Retrieved user role: $userRole")
                        if (userRole.equals("AAZE", ignoreCase = true)) {
                            Log.d("NAVIGATION_DEBUG", "Navigating to SubmitFeaturedChallengeScreen")
                            navController.navigate("submitFeaturedChallenge")
                        } else  if (userRole.equals("ADMIN", ignoreCase = true)) {
                            Log.d("NAVIGATION_DEBUG", "Navigating to SubmitChallengeScreen")
                            navController.navigate("submitChallenge")
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Submit Challenge")
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF211312))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {

                Spacer(modifier = Modifier.height(10.dp))
                Text("  Last Month's Winners", style = MaterialTheme.typography.h6, color = Color.White) // Set text color to white)
                FeaturedWinnersSection(navController = navController)
                Spacer(modifier = Modifier.height(18.dp))

                // Featured Challenge Section ("Challenge of the Month")
                Text(
                    text = "Challenge of the Month",
                    style = MaterialTheme.typography.h6,
                    color = Color.White // Set text color to white
                )

                Spacer(modifier = Modifier.height(18.dp))
                val featuredChallengeState = remember { mutableStateOf<ChallengeDTO?>(null) }

                LaunchedEffect(activeChallenges, featuredChallengeTitles) {
                    val matchingFeaturedChallenges = activeChallenges.filter { it.featured && featuredChallengeTitles.contains(it.title) }

                    Log.d("FeaturedChallenges", "✅ Matching Featured Challenges: $matchingFeaturedChallenges")

                    featuredChallengeState.value = matchingFeaturedChallenges.firstOrNull()

                    if (featuredChallengeState.value != null) {
                        Log.d("FeaturedChallenges", "✅ Selected Featured Challenge: ${featuredChallengeState.value!!.title}")
                    } else {
                        Log.e("FeaturedChallenges", "❌ No Featured Challenge Selected!")
                    }
                }



                val selectedFeaturedChallenge = featuredChallenge // Store the value in a local variable

                if (selectedFeaturedChallenge == null) {
                    if (featuredChallengeTitles.isEmpty() || activeChallenges.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White) // Show loading animation
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No active featured challenge", color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))


                            }
                        }
                    }
                } else {
                    Log.d("FeaturedChallenges", "✅ Displaying Featured Challenge: ${selectedFeaturedChallenge.title}")
                    FeaturedChallengeBanner(
                        challenge = selectedFeaturedChallenge, // Now it's correctly casted
                        onClick = { onChallengeClick(selectedFeaturedChallenge) }  // Navigate when clicked
                    )
                }






                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.height(16.dp))

                LatestChallengesSection(navController, weeklyChallenges)


// Initialize as an empty list until you fetch real data:
                val globalLeaderboard =
                    remember { mutableStateOf(emptyList<GlobalLeaderboardEntry>()) }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Ranking", style = MaterialTheme.typography.h6, color = Color.White) // Set text color to white)
                GlobalLeaderboardSection(navController = navController)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Rules and prizes", style = MaterialTheme.typography.h6, color = Color.White) // Set text color to white)
                RankingRulesCollapsible()
                Spacer(modifier = Modifier.height(16.dp))
                // Prizes Section
                PrizesSection()

            }
        }
    }
}


@Composable
fun FeaturedChallengeBanner(challenge: ChallengeDTO, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp), // Ensuring border and shape are consistent
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() }
            .border(12.dp, Color(0xFF876232), shape = RoundedCornerShape(10.dp)), // Border applied to Card
        elevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp)) // Ensure the image respects the border shape
        ) {
            Image(
                painter = rememberImagePainter(data = challenge.imageUrl),
                contentDescription = challenge.title,
                modifier = Modifier.fillMaxSize(), // Ensure the image fills the Box
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)), // Dark overlay
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.h5, // Smaller font size
                    color = Color.White,
                    textAlign = TextAlign.Center, // Ensures text is centered inside the Box
                    modifier = Modifier.fillMaxWidth() // Makes text span full width for centering
                )
            }
        }
    }
}


@Composable
fun ChallengeCard(challenge: ChallengeDTO, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(250.dp)
            .clickable { onClick() },
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFFF5E1C0)) // Custom background color
                .padding(8.dp)
        ) {
            Image(
                painter = rememberImagePainter(data = challenge.imageUrl),
                contentDescription = challenge.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(challenge.title, style = MaterialTheme.typography.subtitle1)
            Text("Deadline: ${challenge.deadline}", style = MaterialTheme.typography.caption)
            Button(
                onClick = { onClick() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF876232), // Custom background color
                    contentColor = Color.White // Text color
                )
            ) {
                Text("Participate")
            }

        }
    }
}
@Composable
fun LeaderboardSection(
    leaderboard: List<GlobalLeaderboardEntry> = emptyList(),
    navController: NavController,
    currentUserEmail: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF328662), Color(0xFF50A278)) // Gradient green
                ),
                shape = RoundedCornerShape(8.dp) // Rounded button shape
            )
            .padding(16.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0x7A328662), Color(0x974C9C73)) // Gradient green
                    ),
                    shape = RoundedCornerShape(1.dp) // Rounded button shape
                )
                .heightIn(min = 100.dp, max = 400.dp) // Ensure it doesn’t go infinite
                .padding(16.dp)
        ) {
            Text(
                text = "🏆 Leaderboard",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Use LazyColumn for better scrolling management
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(leaderboard) { index, entry ->
                    GlobalLeaderboardItem(
                        rank = index + 1,
                        entry = entry,
                        navController = navController,
                        currentUserEmail = currentUserEmail,
                        // Use the username if available; otherwise fall back to the email.
                        author = entry.username ?: entry.userEmail
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalLeaderboardItem(rank: Int, entry: GlobalLeaderboardEntry, navController: NavController, currentUserEmail: String,   author: String,) {
    val medal = when (rank) {
        1 -> "🥇" // Gold for 1st place
        2 -> "🥈" // Silver for 2nd place
        3 -> "🥉" // Bronze for 3rd place
        else -> "$rank." // Numeric rank for others
    }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val isCurrentUser = author.equals(currentUserEmail, ignoreCase = true)
    val borderColor = if (isCurrentUser) Color(0xFF00C853) else Color.Transparent // Green for current user
    val backgroundColor = if (isCurrentUser) Color(0xFFE8F5E9) else Color.White  // Light green background

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable {
                coroutineScope.launch {
                    val userId = AuthPreferences.fetchUserIdByEmail(author, context)
                    if (userId != null) {
                        navController.navigate("authorDetail/$userId")
                    } else {
                        // Handle error (e.g., show a Toast message)
                        // Example:
                        // Toast.makeText(context, "User profile not found.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Rank & Medal
            Text(
                text = medal,
                modifier = Modifier.width(40.dp),
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
            )

            // User Email (Weighted for proper spacing)
            Text(
                text = author,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.subtitle1.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp // Adjust this value to your desired size
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )


            // Total Points
            Text(
                text = "${entry.totalPoints} pts",
                color = Color(0xFF1E88E5), // Blue color for emphasis
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}


@Composable
fun GlobalLeaderboardSection(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var leaderboard by remember { mutableStateOf<List<GlobalLeaderboardEntry>>(emptyList()) }
    var currentUserEmail by remember { mutableStateOf<String?>(null) }
    var currentUserRank by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) } // Controls leaderboard visibility

    val authService = ApiClient.getAuthService(context)
    val challengeApi = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)

    // Function to fetch leaderboard AFTER email is set
    fun fetchLeaderboard(api: ChallengeApi, userEmail: String) {
        coroutineScope.launch {
            try {
                leaderboard = api.getGlobalLeaderboard().take(50)
                val userRank = leaderboard.indexOfFirst { it.userEmail.equals(userEmail, ignoreCase = true) }
                currentUserRank = if (userRank != -1) userRank + 1 else null
                Log.d("GlobalLeaderboard", "User Rank Found: $currentUserRank")
            } catch (e: Exception) {
                Log.e("GlobalLeaderboard", "Error fetching leaderboard: ${e.localizedMessage}")
            }
        }
    }

    // Fetch user email first
    LaunchedEffect(Unit) {
        authService.getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        currentUserEmail = user.email ?: ""
                        Log.d("GlobalLeaderboard", "Fetched User Email: $currentUserEmail")
                        fetchLeaderboard(challengeApi, currentUserEmail!!)
                    }
                } else {
                    Log.e("GlobalLeaderboard", "Failed to fetch user email: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("GlobalLeaderboard", "Error fetching user email: ${t.localizedMessage ?: "Unknown error"}")
            }
        })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp), // Rounded corners for a smooth look
        elevation = 8.dp, // Adds shadow for depth
        backgroundColor = Color(0x54754A1B) // Light gray background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Button to show the current user's rank if they are ranked
            if (currentUserEmail != null && currentUserRank != null) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Your Rank: $currentUserRank", Toast.LENGTH_SHORT)
                            .show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, shape = RoundedCornerShape(8.dp)), // Subtle shadow
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF876232), // Custom gold-like color
                        contentColor = Color.White
                    )
                ) {
                    Text("🏆 View My Rank", fontWeight = FontWeight.Bold)
                }
            } else if (currentUserEmail != null) {
                Text(
                    text = "🚫 You are not currently ranked.",
                    style = MaterialTheme.typography.subtitle1.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape = RoundedCornerShape(8.dp)), // Shadow effect
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent, // Transparent for gradient effect
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues() // Removes default button padding
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF328662), Color(0xFF50A278)) // Gradient green
                            ),
                            shape = RoundedCornerShape(8.dp) // Rounded button shape
                        )
                        .padding(vertical = 12.dp, horizontal = 16.dp) // Padding inside the button
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (expanded) "⬆ Hide Leaderboard" else "⬇ Show Leaderboard",
                        fontWeight = FontWeight.Bold
                    )
                }
            }


            AnimatedVisibility(visible = expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 400.dp) // Prevent infinite height issue
                ) {
                    LeaderboardSection(
                        leaderboard = leaderboard,
                        navController = navController,
                        currentUserEmail = currentUserEmail ?: "" // Ensures a non-null email
                    )
                }
            }

        }
    }
}

@Composable
fun RankingRulesCollapsible() {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp), // Rounded corners
        elevation = 8.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient( // Gradient background
                        colors = listOf(Color(0xFF866232), Color(0xFFBB852A))
                    )
                )
                .padding(16.dp)
                .animateContentSize() // Smooth expand/collapse animation
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📜 Ranking Rules",
                    style = MaterialTheme.typography.h6.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = Color.White
                    )
                }
            }
            if (expanded) {
                Divider(color = Color.White.copy(alpha = 0.6f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "📅 The Leaderboard resets at the beginning of each month.",
                        "🔼 Submissions are ranked based on the number of likes (highest first).",
                        "✅ Only each user’s highest-liked submission per challenge** is counted.",
                        "🏆 Scoring Breakdown (per user’s best submission):",
                        "   • 🥇 1st place (best submission in challenge) - 100% of the challenge points.",
                        "   • 🥈 2nd place (best submission in challenge) - 70% of the challenge points.",
                        "   • 🥉 3rd place (best submission in challenge) - 50% of the challenge points.",
                        "   • 🎖️ Other ranked submissions (best of each user) - 10% of the challenge points.",
                        "⚠️ A user cannot earn multiple ranking positions in a challenge.",
                        "⚖️ If there’s a tie: the older submitted, the prior."
                    ).forEach { rule ->
                        Text(
                            text = rule,
                            style = MaterialTheme.typography.body1.copy(color = Color.White),
                            textAlign = TextAlign.Justify
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun PrizesSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp), // Rounded corners for modern look
        elevation = 8.dp,
        backgroundColor = Color.Transparent // Transparent to allow gradient
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF00897B), Color(0xFF5FA043)) // Golden gradient
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "🏆 Monthly Prizes",
                style = MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                "🥇 First Place: Gold profile badge +\n      Gold border recipes",
                "🥈 Second Place: Silver profile badge +\n" +
                        "      Silver border recipes",
                "🥉 Third Place: Bronze profile badge +\n" +
                        "      Bronze border recipes"
            ).forEach { prize ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prize,
                        style = MaterialTheme.typography.body1.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun FeaturedWinnersSection(navController: NavController) {
    val context = LocalContext.current
    var winners by remember { mutableStateOf<List<FeaturedWinner>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        try {
            val api = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)
            winners = api.getFeaturedWinners()
        } catch (e: Exception) {
            Log.e("FeaturedWinners", "Error fetching winners: ${e.localizedMessage}")
        }
    }

    if (winners.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp), // Rounded corners
            elevation = 8.dp,
            backgroundColor = Color(0xFFF5E1C0) // Light warm background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient( // Gradient background for a polished look
                        colors = listOf(Color(0xFF876232), Color(0xB9211312))
                    ))
                    .padding(16.dp)
            ) {
                Text(
                    text = "🏆 Top Winners of Last Month",
                    style = MaterialTheme.typography.h6,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))

                winners.forEachIndexed { index, winner ->

                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                val userId = AuthPreferences.fetchUserIdByEmail(winner.userEmail, context)
                                if (userId != null) {
                                    navController.navigate("authorDetail/$userId")
                                } else {
                                    Toast.makeText(context, "User profile not found.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Assign medals for top 3 winners
                            val medal = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "${index + 1}."
                            }
                            Text(
                                text = "$medal ${winner.username ?: winner.userEmail}",
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = "${winner.totalPoints} pts",
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                    if (index < winners.size - 1) {
                        Divider(color = Color.Gray.copy(alpha = 0.3f)) // Separators
                    }
                }
            }
        }
    }
}
@Composable
fun LatestChallengesSection(
    navController: NavController, // ✅ Pass the NavController
    weeklyChallenges: List<ChallengeDTO>
) {
    var selectedFilter by remember { mutableStateOf("None") } // Default filter
    var sortedChallenges by remember { mutableStateOf(weeklyChallenges) }

    // Sort challenges when the filter is changed
    LaunchedEffect(selectedFilter, weeklyChallenges) {
        sortedChallenges = when (selectedFilter) {
            "Score" -> weeklyChallenges.sortedByDescending { it.points }
            "Deadline" -> weeklyChallenges.sortedBy { it.deadline }
            else -> weeklyChallenges
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title
            Text(
                "Latest Challenges",
                style = MaterialTheme.typography.h6,
                color = Color.White
            )

            // Dropdown Filter
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF3C2415),
                        contentColor = Color.White
                    )
                ) {
                    Text("Filter: $selectedFilter")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(onClick = {
                        selectedFilter = "None"
                        expanded = false
                    }) {
                        Text("Remove Filter")
                    }
                    DropdownMenuItem(onClick = {
                        selectedFilter = "Score"
                        expanded = false
                    }) {
                        Text("Sort by Score")
                    }
                    DropdownMenuItem(onClick = {
                        selectedFilter = "Deadline"
                        expanded = false
                    }) {
                        Text("Sort by Deadline")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Challenges Row
        LazyRow {
            items(sortedChallenges) { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    onClick = {
                        // ✅ Navigate to challenge details screen
                        navController.navigate("challengeDetail/${challenge.id}")
                    }
                )
            }
        }
    }
}
