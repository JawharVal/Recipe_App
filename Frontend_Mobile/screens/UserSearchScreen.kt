package com.example.recipeapp.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.Book
import com.example.recipeapp.utils.BookRepository
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Move the User data class to the top level
data class User(
    val username: String,
    val displayName: String,
    val followersCount: Int,
    val profilePictureUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    navController: NavController,
    recipes: MutableList<Recipe>
) {
    val context = LocalContext.current
    val authService = ApiClient.getAuthService(context)
    val coroutineScope = rememberCoroutineScope()

    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }

    var allUsers by remember { mutableStateOf<List<UserDTO>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var avatarUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // 🔹 Stores logged-in user's followers and following IDs
    var loggedInUserId by remember { mutableStateOf<Long?>(null) }
    var followersList by remember { mutableStateOf<List<Long>>(emptyList()) }
    var followingList by remember { mutableStateOf<List<Long>>(emptyList()) }

    // 🔹 UI Filter: "All", "Followers", "Following"
    var selectedFilter by remember { mutableStateOf("All") }

    // Fetch users from backend
    LaunchedEffect(Unit) {
        authService.getAllUsers().enqueue(object : Callback<List<UserDTO>> {
            override fun onResponse(call: Call<List<UserDTO>>, response: Response<List<UserDTO>>) {
                if (response.isSuccessful) {
                    allUsers = response.body().orEmpty()
                } else {
                    Log.e("UserSearchScreen", "Failed to fetch users: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<UserDTO>>, t: Throwable) {
                Log.e("UserSearchScreen", "Error fetching users: ${t.localizedMessage}")
            }
        })

        authService.getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { userDto ->
                        loggedInUserId = userDto.id
                        followersList = userDto.followerIds.orEmpty()
                        followingList = userDto.followingIds.orEmpty()

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
                    Log.e("UserSearchScreen", "Failed to fetch profile: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("UserSearchScreen", "Error fetching profile: ${t.localizedMessage}")
            }
        })
    }

    // Filter users based on the selected filter
    // Filter users based on the selected filter
    val filteredUsers = when (selectedFilter) {
        "Followers" -> allUsers.filter { it.id in followersList }
        "Following" -> allUsers.filter { it.id in followingList }
        else -> if (searchQuery.isBlank()) {
            allUsers.filter { it.followerCount ?: 0 > 0 } // Show only users with followers if no search query
        } else {
            allUsers // Show all users if searching
        }
    }

// Apply search filtering
    val userListToShow = filteredUsers.filter {
        val username = it.username.orEmpty()
        val email = it.email.orEmpty()
        searchQuery.isBlank() || username.contains(searchQuery, ignoreCase = true) || email.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.followerCount ?: 0 }

    var showAddOptionsModal by remember { mutableStateOf(false) }
    var isSheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)


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

    // UI Layout
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
                .padding(16.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            Text(
                text = "Search for users",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search for users...", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search Icon",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF876232),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF876232),
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 Filter Buttons
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("All", "Followers", "Following").forEach { filter ->
                    Button(
                        onClick = { selectedFilter = filter },
                        colors = if (selectedFilter == filter) {
                            ButtonDefaults.buttonColors(containerColor = Color(0xFF876232))
                        } else {
                            ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        }
                    ) {
                        Text(text = filter)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = when (selectedFilter) {
                    "Followers" -> "Your Followers"
                    "Following" -> "You Are Following"
                    else -> "Most Followed Users"
                },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 LazyColumn to list users
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(userListToShow) { userDto ->
                    UserCardBig(
                        userDto = userDto,
                        onClick = { selectedUserId ->
                            if (selectedUserId != null) {
                                navController.navigate("authorDetail/$selectedUserId")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
@Composable
fun UserCardBig(
    userDto: UserDTO,
    onClick: (Long?) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick(userDto.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Use the updated UserAvatar composable.
            UserAvatar(
                imageUriString = userDto.imageUri,
                username = userDto.username
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = userDto.username ?: "Unknown",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "@ ${userDto.username ?: "Unknown"}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                val subType = userDto.subscriptionType?.uppercase()
                if (subType == "PLUS" || subType == "PRO") {
                    Spacer(modifier = Modifier.height(4.dp))
                    SubscriptionBadge(subscriptionType = subType)
                }
            }
            Text(
                text = "${userDto.followerCount ?: 0} Followers",
                color = Color(0xFFC0822D),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// Displays a small badge showing the user's subscription type (PLUS or PRO).
@Composable
fun SubscriptionBadge(subscriptionType: String) {
    val bgColor = when (subscriptionType) {
        "PLUS" -> Color(0xFF00C853)  // Green for PLUS
        "PRO" -> Color(0xFFFF9800)   // Orange for PRO
        else -> Color.Gray
    }
    Box(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = subscriptionType,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
@Composable
fun UserAvatar(
    imageUriString: String?,
    username: String?,
    modifier: Modifier = Modifier.size(56.dp)
) {
    var avatarUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    LaunchedEffect(imageUriString) {
        if (!imageUriString.isNullOrEmpty()) {
            avatarUri = Uri.parse(imageUriString)
            Log.d("UserAvatar", "Using backend imageUri: $avatarUri")
        } else {
            avatarUri = null
            Log.d("UserAvatar", "No valid imageUri. Using initials instead.")
        }
    }

    Surface(
        shape = CircleShape,
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (avatarUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = avatarUri),
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                // Show initials if avatarUri is null or empty
                val initials = username?.take(2)?.uppercase() ?: "NN"
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
