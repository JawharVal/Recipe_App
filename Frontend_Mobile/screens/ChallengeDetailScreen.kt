// File: ChallengeDetailScreen.kt
package com.example.recipeapp.screens

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.network.RecipeService
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.AuthPreferences.fetchUserIdByEmail
import com.example.recipeapp.utils.ChallengeApi
import com.example.recipeapp.utils.ChallengeDTO
import com.example.recipeapp.utils.ChallengeRepository
import com.example.recipeapp.utils.ChallengesViewModelFactory
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO
import com.example.recipeapp.viewmodel.ChallengesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(challengeId: Long, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Create API instances using the authenticated retrofit instance.
    val challengeApi: ChallengeApi = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)
    val challengeRepository = ChallengeRepository(challengeApi)
    val factory = ChallengesViewModelFactory(challengeRepository)
    val viewModel: ChallengesViewModel = viewModel(factory = factory)

    // State for challenge details, loading and error messages.
    var challengeDetail by remember { mutableStateOf<ChallengeDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // State for submitted recipes.
    var submittedRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }

    // Modal bottom sheet state for recipe submission.
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    var currentUserEmail by remember { mutableStateOf("") }
    val authService = ApiClient.getAuthService(context)

    // ✅ Define the leaderboard refresh function inside `ChallengeDetailScreen`
    fun refreshGlobalLeaderboard() {
        coroutineScope.launch {
            try {
                val newLeaderboard = withContext(Dispatchers.IO) { challengeApi.getGlobalLeaderboard() }
                Log.d("GlobalLeaderboard", "Leaderboard updated: $newLeaderboard")
            } catch (e: Exception) {
                Log.e("GlobalLeaderboard", "Error refreshing leaderboard: ${e.localizedMessage}")
            }
        }
    }


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
// Define state for current user's email and username
    var currentUserUsername by remember { mutableStateOf("") }

// Fetch user profile and set both email and username
    LaunchedEffect(Unit) {
        authService.getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        currentUserEmail = user.email ?: ""
                        currentUserUsername = user.username ?: ""
                        Log.d("ChallengeDetailScreen", "Fetched User - Email: $currentUserEmail, Username: $currentUserUsername")
                    }
                } else {
                    Log.e("ChallengeDetailScreen", "Failed to fetch user: ${response.code()} ${response.message()}")
                }
            }
            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("ChallengeDetailScreen", "Error fetching user: ${t.localizedMessage ?: "Unknown error"}")
            }
        })
    }
    // Fetch challenge details and submitted recipes.
    LaunchedEffect(challengeId) {
        try {
            // Get challenge details from the view model.
            challengeDetail = viewModel.getChallengeById(challengeId)
            // Wait a bit for data consistency.
            delay(200)
            // Fetch submitted recipes directly.
            submittedRecipes = withContext(Dispatchers.IO) {
                challengeApi.getSubmittedRecipes(challengeId)
            }
            Log.d("ChallengeDetailScreen", "Fetched submitted recipes: $submittedRecipes")
        } catch (e: Exception) {
            errorMessage = "Error fetching submitted recipes: ${e.localizedMessage}"
            Log.e("ChallengeDetailScreen", errorMessage, e)
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // Compute leaderboard rankings based on the likes count and group by author.
    // ✅ Only include users who have at least 1 like
    // Build a leaderboard as a list of Triple(author, totalLikes, earliestId)
    val leaderboard = submittedRecipes
        .groupBy { it.author.lowercase().trim() }   // Group by normalized author
        .map { (author, recipes) ->
            val totalLikes = recipes.sumOf { it.likes }
            // Determine the smallest recipe id for this author (as a proxy for the oldest submission)
            val earliestId = recipes.minByOrNull { it.id ?: Long.MAX_VALUE }?.id ?: Long.MAX_VALUE
            Triple(author, totalLikes, earliestId)
        }
        .filter { it.second > 0 } // Only include users with > 0 likes
        .sortedWith(
            // First sort by total likes (desc) then by earliest id (asc)
            compareByDescending<Triple<String, Int, Long>> { it.second }
                .thenBy { it.third }
        )


    val normalizedCurrentUsername = currentUserUsername.lowercase().trim()
    Log.d("ChallengeDetailScreen", "Current user username normalized: '$normalizedCurrentUsername'")
    leaderboard.forEach { (author, likes) ->
        Log.d("ChallengeDetailScreen", "Leaderboard entry - author: '$author', likes: $likes")
    }
// Get the best submission per user using a comparator that treats
// higher likes as better, and in case of tie, the submission with the smaller id as better.
    val bestSubmissions = submittedRecipes
        .groupBy { it.author } // Group by author
        .mapValues { entry ->
            entry.value.maxWithOrNull(
                // For two submissions with equal likes, thenByDescending makes the one with the smaller id win.
                compareBy<Recipe> { it.likes }
                    .thenByDescending { it.id ?: Long.MIN_VALUE }
            )!!
        }
        .values
        .sortedWith(
            // For display, sort descending by likes, and if tied, sort ascending by id (so smaller id comes first)
            compareByDescending<Recipe> { it.likes }
                .thenBy { it.id ?: Long.MAX_VALUE }
        )

// Extract the Top 3 Best Recipes
    val top3Recipes = bestSubmissions.take(3)


    // Create a LazyListState for the leaderboard list
    val listState = rememberLazyListState()

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            RecipeSelectionModal(
                onRecipeSubmitted = { updatedChallenge ->
                    // When a recipe is submitted, update the submittedRecipes list.
                    coroutineScope.launch {
                        try {
                            submittedRecipes = withContext(Dispatchers.IO) {
                                challengeApi.getSubmittedRecipes(challengeId)
                            }
                        } catch (e: Exception) {
                            Log.e("ChallengeDetailScreen", "Error updating submitted recipes: ${e.localizedMessage}")
                        }
                    }
                },
                onDismiss = { coroutineScope.launch { sheetState.hide() } },
                challengeId = challengeId
            )
        }
    ) {
        // Set overall Scaffold background to dark (0xFF211312)
        Scaffold(
            backgroundColor = Color(0xFF211312),
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
        ) { paddingValues ->
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Color(0xFF00897B)) }
                }
                errorMessage.isNotEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) { Text(errorMessage, color = MaterialTheme.colors.error) }
                }
                challengeDetail != null -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Challenge image with rounded corners and gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = rememberImagePainter(data = challengeDetail!!.imageUrl),
                                    contentDescription = challengeDetail!!.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color(0xAA000000))
                                            )
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Title: ${challengeDetail!!.title}",
                                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Description: ${challengeDetail!!.description}",
                                style = MaterialTheme.typography.body1,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Deadline: ${challengeDetail!!.deadline}",
                                style = MaterialTheme.typography.caption,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Max Points: ${challengeDetail!!.points}",
                                style = MaterialTheme.typography.caption,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Submissions Allowed: ${challengeDetail!!.maxSubmissions}",
                                style = MaterialTheme.typography.caption,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { coroutineScope.launch { sheetState.show() } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF876232),
                                    contentColor = Color.White
                                )
                            ) { Text("Submit Recipe") }
                        }
                        if (top3Recipes.isNotEmpty()) {
                            item {
                                Text(
                                    "🥇 Top 3 Best Recipes",
                                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                                )
                            }
                            top3Recipes.forEachIndexed { index, recipe ->
                                item {
                                    TopRecipeCard(rank = index + 1, recipe = recipe, navController = navController)
                                }
                            }
                        }

                        if (submittedRecipes.isNotEmpty()) {
                            item {
                                Text(
                                    "Submitted Recipes",
                                    style = MaterialTheme.typography.h6.copy(color = Color(0xFF00897B)),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(submittedRecipes) { recipe ->
                                SubmittedRecipeCard(
                                    recipe = recipe,
                                    navController = navController,
                                    onRecipeLikeUpdate = { recipeId, isLiked, newLikes ->
                                        Log.d("ChallengeDetailScreen", "Updating submittedRecipes for Recipe ID: $recipeId")
                                        submittedRecipes = submittedRecipes.map { r ->
                                            if (r.id == recipeId) r.copy(
                                                likedByUser = isLiked,
                                                likes = newLikes,
                                                notes = r.notes ?: ""
                                            ) else r
                                        }
                                        refreshGlobalLeaderboard()
                                    }
                                )
                            }
                        }
                        Log.d("ChallengeDetailScreen", "Current User Email: $currentUserEmail")
                        Log.d("ChallengeDetailScreen", "Leaderboard: $leaderboard")

                        item { Spacer(modifier = Modifier.height(24.dp)) }
                        val normalizedCurrentUserEmail = currentUserEmail.lowercase()

// Add logs to inspect the values:
                        Log.d("ChallengeDetailScreen", "Current user email: '$currentUserEmail', normalized: '$normalizedCurrentUserEmail'")
                        leaderboard.forEach { (author, likes) ->
                            Log.d("ChallengeDetailScreen", "Leaderboard entry - author: '$author', likes: $likes")
                        }
                        if (leaderboard.isNotEmpty()) {

                            item {

                                if (leaderboard.any { it.first == normalizedCurrentUsername && normalizedCurrentUsername.isNotEmpty() }) {
                                    Button(
                                        onClick = {
                                            val index = leaderboard.indexOfFirst { it.first == normalizedCurrentUsername }
                                            Log.d("ChallengeDetailScreen", "User Rank Index: $index")
                                            if (index != -1) {
                                                coroutineScope.launch {
                                                    // Adjust index as needed
                                                    val leaderboardStartIndex = submittedRecipes.size + 5
                                                    Log.d("ChallengeDetailScreen", "Scrolling to item: ${leaderboardStartIndex + index}")
                                                    listState.animateScrollToItem(leaderboardStartIndex + index)
                                                }
                                            } else {
                                                Toast.makeText(context, "You are not ranked yet!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color(0xFF00897B),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Show My Ranking", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Log.d("ChallengeDetailScreen", "Current user '$normalizedCurrentUsername' not found in leaderboard")
                                }
                            }
                            item {
                                Text(
                                    "Leaderboard",
                                    style = MaterialTheme.typography.h6.copy(color = Color(0xFF00897B)),

                                    )
                            }
                            // "Show My Ranking" button above the leaderboard

                            items(leaderboard) { (author, totalLikes) ->
                                LeaderboardItem(
                                    rank = leaderboard.indexOfFirst { it.first == author } + 1,
                                    author = author,
                                    totalLikes = totalLikes,
                                    currentUserEmail = currentUserEmail,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) { Text("No details available", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun RecipeSelectionModal(
    onRecipeSubmitted: (ChallengeDTO) -> Unit,
    onDismiss: () -> Unit,
    challengeId: Long
) {
    val context = LocalContext.current
    val recipeService = ApiClient.getRetrofit(context).create(RecipeService::class.java)
    val challengeApi = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)

    var recipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var submittedRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val token = "Bearer ${AuthPreferences.getToken(context)}"
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            // Fetch user's recipes.
            val userRecipes = withContext(Dispatchers.IO) {
                recipeService.getUserRecipess(token)
            }
            recipes = userRecipes

            // Fetch all challenges and collect submitted recipes.
            val challenges = withContext(Dispatchers.IO) {
                challengeApi.getAllChallenges()
            }
            submittedRecipes = challenges.flatMap { it.submittedRecipes }
        } catch (e: Exception) {
            errorMessage = "Error fetching recipes: ${e.localizedMessage}"
            Log.e("RecipeSelectionModal", errorMessage, e)
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0xFF424242), shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("Select a Recipe", style = MaterialTheme.typography.h6.copy(color = Color(0xFFB2FF59)))
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFFB2FF59))
        } else if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error)
        } else {
            LazyColumn {
                items(recipes) { recipe ->
                    val alreadySubmitted = submittedRecipes.any { it.id == recipe.id }
                    RecipeCard(recipe = recipe, isDisabled = alreadySubmitted) {
                        if (!alreadySubmitted) {
                            coroutineScope.launch {
                                try {
                                    val updatedChallenge = withContext(Dispatchers.IO) {
                                        challengeApi.submitRecipe(challengeId, recipe.id!!)
                                    }
                                    withContext(Dispatchers.Main) {
                                        onRecipeSubmitted(updatedChallenge)
                                        Toast.makeText(context, "Recipe submitted!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Max submissions Reached.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Text("Cancel")
        }
    }
}

@Composable
fun RecipeCard(recipe: Recipe, isDisabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = !isDisabled) { onClick() },
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .background(if (isDisabled) Color(0xFFB71C1C) else Color(0xFF2C2C2C)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberImagePainter(data = recipe.imageUri),
                contentDescription = recipe.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(end = 8.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Title: ${recipe.title}", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold), color = Color.White)
                if (isDisabled) {
                    Text("Already Submitted", color = Color.White, style = MaterialTheme.typography.caption)
                }
            }
            if (!isDisabled) {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF00897B),
                        contentColor = Color.White
                    )
                ) { Text("Select") }
            }
        }
    }
}

@Composable
fun SubmittedRecipeCard(
    recipe: Recipe,
    navController: NavController,
    onRecipeLikeUpdate: (Long, Boolean, Int) -> Unit
) {
    val context = LocalContext.current
    val token = AuthPreferences.getToken(context) ?: ""
    val recipeService = ApiClient.getRetrofit(context).create(RecipeService::class.java)
    val coroutineScope = rememberCoroutineScope()

    var likesCount by remember { mutableStateOf(recipe.likes) }
    var liked by remember { mutableStateOf(recipe.likedByUser) }
    fun refreshGlobalLeaderboard() {
        coroutineScope.launch {
            try {
                val api = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)
                val newLeaderboard = api.getGlobalLeaderboard()
                Log.d("GlobalLeaderboard", "Refreshed leaderboard: $newLeaderboard")
            } catch (e: Exception) {
                Log.e("GlobalLeaderboard", "Error refreshing leaderboard: ${e.localizedMessage}")
            }
        }
    }
    LaunchedEffect(recipe.id) {
        Log.d("SubmittedRecipeCard", "Fetching latest likes for Recipe ID: ${recipe.id}")
        recipeService.getRecipeById(recipe.id!!).enqueue(object : Callback<Recipe> {
            override fun onResponse(call: Call<Recipe>, response: Response<Recipe>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        likesCount = it.likes
                        liked = it.likedByUser
                        Log.d("SubmittedRecipeCard", "Updated from server - Likes: ${it.likes}, Liked: ${it.likedByUser}")
                    }
                } else {
                    Log.e("SubmittedRecipeCard", "Error fetching recipe likes: ${response.code()} ${response.message()}")
                }
            }
            override fun onFailure(call: Call<Recipe>, t: Throwable) {
                Log.e("SubmittedRecipeCard", "Network error while fetching likes: ${t.message}")
            }
        })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { navController.navigate("recipeDetail/${recipe.id}") },
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFF2C2C2C)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberImagePainter(data = recipe.imageUri),
                contentDescription = recipe.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(end = 8.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Title: ${recipe.title}", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("Author: ${recipe.author}", style = MaterialTheme.typography.subtitle2, color = Color.LightGray)
                Text(
                    text = "Description: ${recipe.notes}",
                    style = MaterialTheme.typography.caption,
                    maxLines = 2,
                    color = Color.LightGray
                )
            }
            IconButton(
                onClick = {
                    val newLikedState = !liked
                    val newLikesCount = if (newLikedState) likesCount + 1 else likesCount - 1
                    liked = newLikedState
                    likesCount = newLikesCount
                    onRecipeLikeUpdate(recipe.id!!, newLikedState, newLikesCount)
                    recipeService.likeRecipe(recipe.id!!, "Bearer $token").enqueue(object : Callback<Recipe> {
                        override fun onResponse(call: Call<Recipe>, response: Response<Recipe>) {
                            if (response.isSuccessful) {
                                response.body()?.let {
                                    likesCount = it.likes
                                    liked = it.likedByUser
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        onRecipeLikeUpdate(recipe.id!!, liked, likesCount)
                                    }, 300)
                                    refreshGlobalLeaderboard()
                                }
                            } else {
                                rollbackUI(newLikedState)
                            }
                        }
                        override fun onFailure(call: Call<Recipe>, t: Throwable) {
                            Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                            rollbackUI(newLikedState)
                        }
                        private fun rollbackUI(newLiked: Boolean) {
                            liked = !newLiked
                            likesCount = if (newLiked) likesCount - 1 else likesCount + 1
                        }
                    })
                }
            ) {
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (liked) Color.Red else Color.Gray
                )
            }
            Text("$likesCount", style = MaterialTheme.typography.subtitle2, color = Color.White)
        }
    }
}

@Composable
fun LeaderboardItem(
    rank: Int,
    author: String, // the author's email
    totalLikes: Int,
    currentUserEmail: String,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color(0xBE00897B) // Gray for other ranks
    }

    val isCurrentUser = author.equals(currentUserEmail, ignoreCase = true)
    val borderColor = if (isCurrentUser) Color(0xFF00C853) else Color.Transparent // Green for current user
    // Use dark gray shades for leaderboard item backgrounds on the dark screen
    val backgroundColor = if (isCurrentUser) Color(0xFF37474F) else Color(0xFF2C2C2C)

    Card(
        shape = MaterialTheme.shapes.medium,
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .border(width = if (isCurrentUser) 3.dp else 0.dp, color = borderColor)
            .clickable {
                coroutineScope.launch {
                    val userId = fetchUserIdByEmail(author, context)
                    if (userId != null) {
                        navController.navigate("authorDetail/$userId")
                    } else {
                        // Handle error if needed
                    }
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = rankColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.subtitle1.copy(color = Color.White)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Author Name
            Text(
                text = author,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.weight(1f),
                color = Color.White
            )
            // Total Likes
            Text(
                text = "$totalLikes ❤️",
                style = MaterialTheme.typography.h6,
                color = rankColor
            )
        }
    }
}
@Composable
fun TopRecipeCard(rank: Int, recipe: Recipe, navController: NavController) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold 🥇
        2 -> Color(0xFFC0C0C0) // Silver 🥈
        3 -> Color(0xFFCD7F32) // Bronze 🥉
        else -> Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { navController.navigate("recipeDetail/${recipe.id}") },
        elevation = 4.dp,
        shape = RoundedCornerShape(10.dp),
        backgroundColor = Color(0xFF2C2C2C)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🏆 Rank Badge
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(rankColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (rank) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> "$rank"
                    },
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Recipe Information
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.title, style = MaterialTheme.typography.h6.copy(color = Color.White))
                Text("Author: ${recipe.author}", color = Color.Gray, fontSize = 12.sp)
                Text("Likes: ${recipe.likes} ❤️", color = Color.White, fontSize = 14.sp)
            }

            // Recipe Image
            Image(
                painter = rememberImagePainter(data = recipe.imageUri),
                contentDescription = recipe.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
fun normalizeAuthor(author: String): String {
    val lower = author.lowercase().trim()
    // If the author string contains "@", assume it’s an email and return it.
    // Otherwise, return the local part from the current user email.
    return if (lower.contains("@")) lower else lower
}
