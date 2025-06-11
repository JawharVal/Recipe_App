package com.example.recipeapp.screens

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.utils.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Profile Screen with a dark theme and golden-brown accent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    context: Context,
    recipes: MutableList<Recipe>
) {
    // State and variables
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
    val plannedRecipes = remember { mutableStateMapOf<LocalDate, SnapshotStateList<Recipe>>() }
    val notes = remember { mutableStateMapOf<LocalDate, SnapshotStateList<Note>>() }
    var user by remember { mutableStateOf<UserDTO?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }
    val authService = ApiClient.getAuthService(context)
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // Google & Firebase
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("832072464275-84unkql8c2h7qa5u27b083k1mb2f6671.apps.googleusercontent.com")
        .requestEmail()
        .build()
    val googleSignInClient: GoogleSignInClient =
        GoogleSignIn.getClient(context, googleSignInOptions)
    val firebaseAuth = FirebaseAuth.getInstance()

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

    // Fetch user details
    LaunchedEffect(Unit) {
        if (AuthPreferences.isLoggedIn(context)) {
            fetchUserDetails(
                context = context,
                onSuccess = { fetchedUser ->
                    user = fetchedUser
                    username = fetchedUser.username ?: "User"
                    isLoading = false

                    // Now fetch favorites
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
        } else {
            errorMessage = "User not logged in."
            isLoading = false
        }
    }

    // Handle system back navigation
    BackHandler {
        navController.navigate("home") {
            popUpTo("profile") { inclusive = true }
        }
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
        // Main content
        Scaffold(
            // Remove the default topBar and build a custom dark top bar
            topBar = {
                CookbooksTopBars(
                    onSettingsClick = { navController.navigate("editProfile") },
                    navController = navController
                )
            },
            bottomBar = {
                // Still use your custom bottom bar but with dark theme
                BottomNavigationBar(
                    navController = navController,
                    selectedTab = "profile",
                    onTabSelected = { /* no-op */ },
                    onAddClick = { showAddOptionsModal = true }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // 1) Banner Section


                // 2) Profile Header Circle
                item {
                    user?.let { userData ->
                        ProfileHeaderCircle(
                            author = userData,
                            isOwnProfile = true,
                            refreshProfile = {
                                fetchUserDetails(
                                    context = context,
                                    onSuccess = { updatedUser -> user = updatedUser },
                                    onFailure = {}
                                )
                            },
                            onAvatarClick = {
                                // Replace "userData.id" with the appropriate identifier (e.g., authorId) as needed.
                                userData.id?.let { authorId ->
                                    navController.navigate("authorDetail/$authorId")
                                }
                            }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // 3) Stats Section (Favorites)
                item {
                    var searchQuery by remember { mutableStateOf("") }
                    DarkStatsAndUserSearch(
                        favoritesCount = favoritesCount,
                        onFavoritesClick = {
                            navController.navigate("favorites")
                        },
                        onSearchForUsersClick = {
                            navController.navigate("userSearch")
                        },
                        navController = navController // ← Add this
                    )

                }

                // 4) Feedback & Settings
                item {
                    DarkFeedbackAndSettings(navController)
                }
// 5) Logout Button
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // 5) Logout Button
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    DarkLogoutButton(
                        onLogout = { showLogoutConfirmation = true }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(34.dp))
                }
// 6) Dangerous actions section
                // ...
// 6) Dangerous actions section
                item {
                    DangerousActionsSection(
                        onDeleteAllRecipes = {
                            val token = AuthPreferences.getToken(context)
                            if (token.isNullOrEmpty()) {
                                Toast.makeText(context, "User token not found.", Toast.LENGTH_SHORT)
                                    .show()
                                return@DangerousActionsSection
                            }
                            recipeRepository.bulkDeleteUserRecipes(token) { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "All recipes deleted.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to delete recipes.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onDeleteAccount = {
                            val userId = user?.id
                            if (userId == null) {
                                Toast.makeText(context, "User not found.", Toast.LENGTH_SHORT)
                                    .show()
                                return@DangerousActionsSection
                            }
                            authService.deleteUser(userId).enqueue(object : Callback<Void> {
                                override fun onResponse(
                                    call: Call<Void>,
                                    response: Response<Void>
                                ) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(
                                            context,
                                            "Account deleted.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navController.navigate("login") {
                                            popUpTo("profile") { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to delete account.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                override fun onFailure(call: Call<Void>, t: Throwable) {
                                    Toast.makeText(
                                        context,
                                        "Failed to delete account.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                        }
                    )
                }
// ...


                item {

                    Footer()
                }


            }

            // **Loading Indicator (Overlayed Above Bottom Bar, Under Top Bar)**
            if (isLoading || isFavoritesLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)) // Slight transparency
                        .padding(paddingValues), // Apply Scaffold padding
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF876232))
                }
            }

            // Logout Confirmation Dialog
            if (showLogoutConfirmation) {
                LogoutConfirmationDialog(
                    onConfirm = {
                        AuthPreferences.clearToken(context)
                        firebaseAuth.signOut()
                        googleSignInClient.signOut().addOnCompleteListener {
                            navController.navigate("login") {
                                popUpTo("profile") { inclusive = true }
                            }
                        }
                        showLogoutConfirmation = false
                    },
                    onDismiss = { showLogoutConfirmation = false }
                )
            }
        }
    }
}

/**
 * Custom top bar styled like in the dark-themed ShoppingScreen.
 */
@Composable
fun ProfileTopBar() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp)
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
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Settings",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { /* Navigate to settings */ },
                tint = Color.White
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
/**
 * A dark-themed banner section, similar to the BannerSection but with black & gold styling.
 */
@Composable
fun DarkBannerSection(navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.dp, bottom = 16.dp), // reduced top padding
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Get the most out of our App",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate("premium") },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF876232),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Start right now",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * A dark-themed stats section to display favorite recipes count.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkStatsAndUserSearch(
    favoritesCount: Int,
    onFavoritesClick: () -> Unit,
    onSearchForUsersClick: () -> Unit,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)  // Ensures children match the tallest child
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1) "Search for Users" Card on the left
        DarkSearchForUsersCard(
            label = "Search for users",
            icon = R.drawable.ic_search,
            onClick = onSearchForUsersClick,
            navController = navController,        // ← Provide this
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )


        Spacer(modifier = Modifier.width(16.dp))

        // 2) Favorites Stat Card on the right
        DarkStatCard(
            icon = android.R.drawable.btn_star_big_on,
            statValue = favoritesCount.toString(),
            statLabel = "Favorite recipes",
            onClick = onFavoritesClick,
            modifier = Modifier
                .weight(1f)           // Takes half the row's width
                .fillMaxHeight()      // Match the row’s height
        )
    }
}

@Composable
fun DarkSearchForUsersCard(
    label: String,
    icon: Int,
    onClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { navController.navigate("userSearch") },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                tint = Color(0xFFC48028),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color(0xFF8F8E8E),

                fontSize = 14.sp,
                textAlign = TextAlign.Center

            )
        }
    }
}
@Composable
fun DarkStatCard(
    icon: Int,
    statValue: String,
    statLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = statLabel,
                tint = Color(0xFFC48028),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statValue,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = statLabel,
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}



/**
 * A stat card with a dark background and gold accents.
 */@Composable
fun DarkStatCard(
    icon: Int,
    statValue: String,
    statLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.5f)  // Occupies 50% width
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = statLabel,
                tint = Color(0xFF876232),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statValue,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = statLabel,
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Dark-themed FeedbackItem used inside FeedbackAndSettings.
 */
@Composable
fun DarkFeedbackItem(
    title: String,
    description: String,
    buttonText: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isSubscribed: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSubscribed) Color.Gray else Color(0xFF876232)
                    ),
                    colors = outlinedButtonColors(
                        containerColor = if (isSubscribed) Color(0xFF2E2E2E) else Color.Transparent,
                        contentColor = if (isSubscribed) Color.Gray else Color(0xFF876232)
                    )
                ) {
                    if (isSubscribed) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Subscribed",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF876232)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unsubscribe",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = buttonText,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF876232)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buttonText,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dark-themed FeedbackAndSettings section with Feedback, Newsletter, and Settings.
 */
@Composable
fun DarkFeedbackAndSettings(navController: NavController) {
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showNewsletterDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val feedbackRepository = remember { FeedbackRepository(context) }
    val newsletterRepository = remember { NewsletterRepository(context) }

    var isSubscribed by remember { mutableStateOf(false) }
    var isLoadingSubscription by remember { mutableStateOf(true) }

    // Check if user is subscribed to the newsletter
    LaunchedEffect(Unit) {
        newsletterRepository.isSubscribed { subscribed ->
            isSubscribed = subscribed
            isLoadingSubscription = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // 1) Feedback
        DarkFeedbackItem(
            title = "Feedback",
            description = "Please let us know what you think about LeGourmand so we can continue to improve the app.",
            buttonText = "Send Feedback",
            icon = Icons.Default.Send,
            onClick = { showFeedbackDialog = true }
        )

        // 2) Newsletter
        DarkFeedbackItem(
            title = "Newsletter",
            description = "Get the latest updates delivered to your inbox ...",
            buttonText = if (isSubscribed) "Unsubscribe" else "Subscribe",
            icon = if (isSubscribed) Icons.Default.Star else Icons.Default.Email,
            onClick = {
                if (!isSubscribed) {
                    showNewsletterDialog = true
                } else {
                    // Unsubscribe
                    Toast.makeText(context, "Unsubscribing...", Toast.LENGTH_SHORT).show()
                    newsletterRepository.unsubscribe { result ->
                        when (result) {
                            is SubscriptionResult.Success -> {
                                isSubscribed = false
                                Toast.makeText(context, "Unsubscribed successfully.", Toast.LENGTH_SHORT).show()
                            }
                            is SubscriptionResult.AlreadySubscribed -> {
                                Toast.makeText(context, "You're already subscribed.", Toast.LENGTH_SHORT).show()
                            }
                            is SubscriptionResult.NotSubscribed -> {
                                Toast.makeText(context, "You were not subscribed.", Toast.LENGTH_SHORT).show()
                            }
                            is SubscriptionResult.Failure -> {
                                Toast.makeText(context, "Failed to unsubscribe. Error code: ${result.errorCode}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            isSubscribed = isSubscribed
        )

        // 3) Settings
        DarkFeedbackItem(
            title = "Membership",
            description = "Manage your membership plans here.",
            buttonText = "Browse plans",
            icon = Icons.Default.Settings,
            onClick = { navController.navigate("premium") },
        )

        // 4) Restart Introduction
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val prefs: SharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("onboarding_completed", false).apply()
                navController.navigate("onboarding") {
                    popUpTo("profile") { inclusive = true }
                }
            },
            border = BorderStroke(1.dp, Color(0xFF876232)),  // Add your border here
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFF000000),    // Red background
                contentColor = Color.White            // White text/icon
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Restart Introduction",
                color = Color(0xFFC07C23),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    // Feedback Dialog
    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onFeedbackSend = { feedback ->
                feedbackRepository.submitFeedback(feedback) { success ->
                    showFeedbackDialog = false
                    if (success) {
                        Toast.makeText(context, "Feedback submitted successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to submit feedback.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Newsletter Dialog
    if (showNewsletterDialog) {
        NewsletterDialog(
            onDismiss = { showNewsletterDialog = false },
            onSubscribe = {
                newsletterRepository.subscribe { result ->
                    when (result) {
                        is SubscriptionResult.Success -> {
                            isSubscribed = true
                            Toast.makeText(context, "Subscribed to newsletter successfully.", Toast.LENGTH_SHORT).show()
                        }
                        is SubscriptionResult.AlreadySubscribed -> {
                            isSubscribed = true
                            Toast.makeText(context, "You're already subscribed.", Toast.LENGTH_SHORT).show()
                        }
                        is SubscriptionResult.NotSubscribed -> {
                            Toast.makeText(context, "You are not subscribed.", Toast.LENGTH_SHORT).show()
                        }
                        is SubscriptionResult.Failure -> {
                            Toast.makeText(context, "Failed to subscribe. Error code: ${result.errorCode}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showNewsletterDialog = false
                }
            },
            onError = { errorMsg ->
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Dark-themed logout button.
 */
@Composable
fun DarkLogoutButton(onLogout: () -> Unit) {
    OutlinedButton(
        onClick = onLogout,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF9B2522)),  // Add your border here
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF000000),    // Red background
            contentColor = Color(0xFF9B2522)            // White text/icon
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Logout,
            contentDescription = "Logout",
            tint = Color(0xFFC91813),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Logout",
            color = Color(0xFFDA1611),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * An error page that uses the dark color scheme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorPageDark(
    message: String,
    navController: NavController, // Added navController parameter
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            CookbooksTopBar(
                onSettingsClick = { navController.navigate("editProfile") },
                navController = navController
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Retry
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF876232)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Retry",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Logout
                OutlinedButton(
                    onClick = onLogout,
                    colors = outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Red
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Function to fetch user details from the backend.
 */
fun fetchUserDetails(
    context: Context,
    onSuccess: (UserDTO) -> Unit,
    onFailure: () -> Unit
) {
    val authService = ApiClient.getRetrofit(context).create(AuthService::class.java)
    authService.getProfile().enqueue(object : Callback<UserDTO> {
        override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    onSuccess(user)
                } ?: run {
                    onFailure()
                }
            } else if (response.code() == 401) {
                AuthPreferences.clearToken(context)
                onFailure()
            } else {
                onFailure()
            }
        }

        override fun onFailure(call: Call<UserDTO>, t: Throwable) {
            onFailure()
        }
    })
}
@Composable
fun DarkSearchForUsersCard(
    label: String,
    icon: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.5f)  // Occupies 50% width; adjust as needed
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                tint = Color(0xFF876232),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangerousActionsSection(
    onDeleteAllRecipes: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteAllRecipesConfirmation by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmation by remember { mutableStateOf(false) }

    Column {
        // Header row: "Dangerous actions" + expand/collapse icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dangerous actions",
                color = Color(0xFFD82D28),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.Red
            )
        }

        // If expanded, show the two dangerous action cards
        if (isExpanded) {
            // Card for "Delete all my recipes"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Delete all my recipes",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This will delete all your recipes and remove them from all cookbooks and meal planners. This action is irreversible as all data is permanently deleted from our servers.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showDeleteAllRecipesConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C2522)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Delete all my recipes", color = Color.White)
                    }
                }
            }

            // Card for "Delete account"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Delete account",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This action cannot be undone. It will permanently delete your account and remove your data from our servers. Any cookbooks you own will also be deleted.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showDeleteAccountConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C2522)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Delete account", color = Color.White)
                    }
                }
            }
        }
    }

    // Confirmation dialogs
    if (showDeleteAllRecipesConfirmation) {
        DeleteAllRecipesConfirmationDialog(
            onConfirm = {
                onDeleteAllRecipes()
                showDeleteAllRecipesConfirmation = false
            },
            onDismiss = { showDeleteAllRecipesConfirmation = false }
        )
    }
    if (showDeleteAccountConfirmation) {
        DeleteAccountConfirmationDialog(
            onConfirm = {
                onDeleteAccount()
                showDeleteAccountConfirmation = false
            },
            onDismiss = { showDeleteAccountConfirmation = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAllRecipesConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirm Deletion",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete all your recipes? This action cannot be undone.",
                color = Color.Gray
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete", color = Color(0xFF9C2522))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1F1F1F)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirm Account Deletion",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete your account? This action cannot be undone.",
                color = Color.Gray
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete", color = Color(0xFF9C2522))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1F1F1F)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirm Logout",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Text(
                text = "Are you sure you want to logout?",
                color = Color.Gray
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Logout", color = Color(0xFF9C2522))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1F1F1F)
    )
}
@Composable
fun Footer() {
    var showDialog by remember { mutableStateOf(false) }
    var dialogContent by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "© 2025 Jawhar M. All rights reserved.",
            color = Color(0xFF737373),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            ClickableText(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF737373))) {
                        append("Imprint")
                    }
                },
                onClick = {
                    dialogContent = """
                        This app is owned and operated by J.M. 
                        Address: Rue La Reserve, Paris, France.
                        Contact: JM@gmail.com
                        VAT ID: DE123456789
                    """.trimIndent()
                    showDialog = true
                }
            )
            Text(text = " - ", color = Color(0xFF737373))

            ClickableText(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF737373))) {
                        append("Privacy Policy")
                    }
                },
                onClick = {
                    dialogContent = """
                        We are committed to protecting your privacy. This Privacy Policy explains how we collect, use, and safeguard your information.
                        
                        1. Information We Collect:
                        - Personal details (name, email, etc.) provided during registration.
                        - Data collected from cookies and analytics tools.
                        
                        2. How We Use Your Data:
                        - To provide and improve our services.
                        - To personalize content and user experience.
                        - To comply with legal obligations.
                        
                        3. Data Protection & Security:
                        - Your data is securely stored and protected from unauthorized access.
                        - We do not sell or share your personal information with third parties without consent.
                       
                    """.trimIndent()
                    showDialog = true
                }
            )
            Text(text = " - ", color = Color(0xFF737373))

            ClickableText(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF737373))) {
                        append("Terms of Service")
                    }
                },
                onClick = {
                    dialogContent = """
                        By using our application, you agree to comply with these Terms of Service.
                        
                        1. Acceptance of Terms:
                        - Your access and use of the app is subject to these terms.
                        - If you do not agree, please do not use the app.
                        
                        2. User Responsibilities:
                        - You must not use our services for any illegal activities.
                        - You are responsible for maintaining the confidentiality of your login credentials.
                        
                        3. Limitations of Liability:
                        - We are not liable for any damages resulting from the use of our services.
                        - We reserve the right to modify or terminate the service at any time.
                        
                        4. Changes to Terms:
                        - We may update these terms occasionally. Continued use of the app constitutes acceptance of any changes.
                       
                    """.trimIndent()
                    showDialog = true
                }
            )
        }
    }

    // Modal Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Information", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text(text = dialogContent, color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "Close", color = Color(0xFF737373))
                }
            },
            containerColor = Color(0xFF1F1F1F)
        )
    }
}

@Composable
fun CookbooksTopBars(onSettingsClick: () -> Unit ,  navController: NavController) {
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

            // Settings Icon on the right
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Settings",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { navController.navigate("editProfile") },
                tint = Color.White
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorPageDarkz(
    message: String,
    navController: NavController, // Added navController parameter
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            CookbooksTopBare(
                onSettingsClick = { navController.navigate("editProfile") },
                navController = navController
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Retry
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF876232)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Retry",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Logout
                OutlinedButton(
                    onClick = onLogout,
                    colors = outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Red
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
@Composable
fun CookbooksTopBare(onSettingsClick: () -> Unit ,  navController: NavController) {
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
