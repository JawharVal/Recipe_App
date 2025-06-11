package com.example.recipeapp.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthService
import com.example.recipeapp.utils.ConfettiEffect
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.StripeManager
import com.example.recipeapp.utils.SubscriptionRequest
import com.example.recipeapp.utils.UserDTO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(navController: NavController, recipes: MutableList<Recipe>, stripeManager: StripeManager) {
    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authService = ApiClient.getAuthService(context)
    val recipeRepository = remember { RecipeRepository(context) }
    val allRecipes = remember { mutableStateListOf<Recipe>() }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // State for Search and Filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilters by remember { mutableStateOf(listOf<String>()) }
    var showAddOptionsModal by remember { mutableStateOf(false) }
    // Available filter options
    val availableFilters = listOf("Vegan", "Dessert", "Quick Meals", "American")
    var isSheetVisible by remember { mutableStateOf(false) }
    // Handle Add Options Modal and Bottom Sheet
    val activity = LocalContext.current as ComponentActivity  // ✅ Ensure it's ComponentActivity
    var showConfetti by remember { mutableStateOf(false) }
    if (showAddOptionsModal) {
        AddOptionsModal(
            navController = navController,
            onDismiss = { showAddOptionsModal = false },
            onAddManually = { isSheetVisible = true }
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        stripeManager.onPaymentSuccess = {
            // Show confetti effect immediately
            showConfetti = true
            // Launch a coroutine to handle the delay and navigation
            coroutineScope.launch {
                delay(3000)  // Delay for 3 seconds
                showConfetti = false
                navController.navigate("profile")
            }
        }
    }

    LaunchedEffect(Unit) {
        authService.getProfile().enqueue(object : Callback<UserDTO> {
            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        plusUnlocked = user.subscriptionType == "PLUS"
                        proUnlocked = user.subscriptionType == "PRO"
                    }
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("PremiumScreen", "Error fetching profile: ${t.localizedMessage}")
            }
        })
    }

// Fetch the current user profile to update subscription status when the screen loads
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
                    Log.e("PremiumScreen", "Failed to fetch profile: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("PremiumScreen", "Error fetching profile: ${t.localizedMessage ?: "Unknown error"}")
            }
        })
    }

    if (proUnlocked) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                message = "Contact support to downgrade from Pro to Plus.",
                actionLabel = "OK"
            )
        }
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
                                tint = Color(0xFF886232)
                            )
                            Spacer(modifier = Modifier.width(8.dp)) // Add some spacing between the logo and title
                            Text("LeGourmand Plus", fontSize = 20.sp, color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
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
                selectedTab = "profile",
                onTabSelected = { /* Handle tab change */ },
                onAddClick = { showAddOptionsModal = true }
            )
        },
        containerColor = Color.Black // Set the scaffold background to black
    ) { innerPadding ->
        Column(

            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black) // Ensure the column background is black
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Choose a plan that suits your needs.", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            PlanCard(
                title = "Free",
                description = "Try before you buy.",
                benefits = listOf(
                    "Store 10 recipes",
                    "3 cookbooks",
                    "3 AI requests per month"
                ),
                planType = "FREE",
                onUnlockClick = { /* No action for Free plan */ },
                isUnlocked = true
            )
            Spacer(modifier = Modifier.height(16.dp))


            PlanCard(
                title = "Plus (449₽/Month)",
                description = "Organize all your recipes (Most popular choice)",
                benefits = listOf("25 recipes", "10 cookbooks", "10 AI requests per month"),
                planType = "PLUS",
                onUnlockClick = { planType ->
                    Log.d("PlanCard", "Unlock button clicked for plan: $planType")
                    coroutineScope.launch {
                        try {
                            Log.d("PlanCard", "Starting checkout process for plan: $planType")
                            stripeManager.startCheckout(planType)  // Start Payment
                            Log.d("PlanCard", "Stripe checkout started successfully for plan: $planType")
                        } catch (e: Exception) {
                            Log.e("PlanCard", "Error starting checkout for plan $planType: ${e.localizedMessage}")
                        }

                        // Wait for Stripe to process the payment (if necessary)
                        kotlinx.coroutines.delay(5000) // Adjust if necessary
                        Log.d("PlanCard", "Delay complete. Refreshing user profile...")

                        // Call Backend to Update Subscription in DB
                        authService.getProfile().enqueue(object : Callback<UserDTO> {
                            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                                Log.d("PlanCard", "getProfile response received: Code ${response.code()}, Message: ${response.message()}")
                                if (response.isSuccessful) {
                                    response.body()?.let { user ->
                                        Log.d("PlanCard", "User subscription type from response: ${user.subscriptionType}")
                                        plusUnlocked = user.subscriptionType == "PLUS" || user.subscriptionType == "PRO"
                                        proUnlocked = user.subscriptionType == "PRO"
                                    }
                                } else {
                                    Log.e("PlanCard", "Failed to refresh subscription: ${response.code()} ${response.message()}")
                                }
                            }

                            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                                Log.e("PlanCard", "Failed to refresh subscription: ${t.localizedMessage}")
                            }
                        })
                    }
                },
                isUnlocked = plusUnlocked,
                isProSelected = proUnlocked
            )


            Spacer(modifier = Modifier.height(16.dp))

            PlanCard(
                title = "Pro (799₽/Month)",
                description = "Unlimited features.",
                benefits = listOf("Unlimited recipes", "Unlimited cookbooks", "Unlimited AI requests"),
                planType = "PRO",
                onUnlockClick = { planType ->
                    coroutineScope.launch {
                        stripeManager.startCheckout(planType)  // ✅ Start Payment

                        // ✅ Wait for Stripe to process the payment
                        kotlinx.coroutines.delay(5000) // Adjust if necessary

                        // ✅ Call Backend to Update Subscription in DB
                        authService.getProfile().enqueue(object : Callback<UserDTO> {
                            override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                                if (response.isSuccessful) {
                                    response.body()?.let { user ->
                                        plusUnlocked = user.subscriptionType == "PLUS" || user.subscriptionType == "PRO"
                                        proUnlocked = user.subscriptionType == "PRO"
                                    }
                                }
                            }

                            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                                Log.e("PlanCard", "Failed to refresh subscription: ${t.localizedMessage}")
                            }
                        })
                    }
                },
                isUnlocked = proUnlocked
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Testimonial Section
            TestimonialSection()

            // Show downgrade message if Pro is selected and Plus is clicked
            if (proUnlocked) {
                LaunchedEffect(Unit) {
                    snackbarHostState.showSnackbar(
                        message = "Contact support to downgrade from Pro to Plus.",
                        actionLabel = "OK"
                    )
                }
            }
        }
        ConfettiEffect(show = showConfetti)
    }
}

@Composable
fun PlanCard(
    title: String,
    description: String,
    benefits: List<String>,
    planType: String, // "FREE", "PLUS", or "PRO"
    onUnlockClick: (String) -> Unit, // Callback to handle subscription logic
    isUnlocked: Boolean = false, // Whether this plan is already unlocked
    isProSelected: Boolean = false // Whether Pro plan is selected
) {
    var isButtonClicked by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color(0xFF252424), // Dark gray card background
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3EBB86)
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray) // Gray description text
            )

            if (!isUnlocked) {
                Button(
                    onClick = {
                        isButtonClicked = true
                        onUnlockClick(planType)
                    },
                    enabled = !isButtonClicked && !(isProSelected && planType == "PLUS"),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isButtonClicked) Color.Gray else Color(0xFFC0873F),
                        contentColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isButtonClicked) {
                            LinearProgressIndicator(
                                progress = 1f,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Unlock now")
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Unlocked",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFF19F36)),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Divider(color = Color.Gray, thickness = 1.dp) // Gray divider

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                benefits.forEach { benefit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Gray // Gray checkmark icon
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White) // White benefit text
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TestimonialSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Quotation Mark Icon
        Icon(
            imageVector = Icons.Default.FormatQuote,
            contentDescription = "Quotation Mark",
            tint = Color.Gray,
            modifier = Modifier.size(32.dp)
        )

        // Testimonial Text
        Text(
            text = "An amazing app with usage of AI for anyone to create and maintain easy cooking at home.",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        // Star Rating
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(5) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star",
                    tint = Color(0xFFC0873F),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // User Count Text
        Text(
            text = "Loved by thousands of cooks worldwide",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
fun refreshUserProfile(authService: AuthService, onResult: (UserDTO?) -> Unit) {
    authService.getProfile().enqueue(object : Callback<UserDTO> {
        override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
            if (response.isSuccessful) {
                onResult(response.body())
            } else {
                Log.e("PremiumScreen", "Failed to fetch profile: ${response.code()} ${response.message()}")
                onResult(null)
            }
        }

        override fun onFailure(call: Call<UserDTO>, t: Throwable) {
            Log.e("PremiumScreen", "Error fetching profile: ${t.localizedMessage ?: "Unknown error"}")
            onResult(null)
        }
    })
}
