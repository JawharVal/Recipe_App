package com.example.recipeapp.navigation



import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.screens.*
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.GeneratedRecipe
import com.example.recipeapp.utils.MealPlanRepository
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.ShoppingListItem
import com.example.recipeapp.utils.ShoppingRepository
import com.example.recipeapp.utils.StripeManager
import kotlinx.coroutines.launch

fun NavGraphBuilder.authenticatedComposable(
    route: String,
    navController: NavController,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(route) { backStackEntry ->
        val context = LocalContext.current
        val isLoggedIn = AuthPreferences.isLoggedIn(context)
        if (isLoggedIn) {
            content(backStackEntry)
        } else {
            // Navigate to login screen
            LaunchedEffect(Unit) {
                navController.navigate("login") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(recipes: MutableList<Recipe>,stripeManager: StripeManager) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
// Initialize ShoppingRepository here
    val shoppingRepository = remember { ShoppingRepository(context) }
    val mealPlanRepository = remember { MealPlanRepository(context) }
    // Manage a list of generated recipes
    val generatedRecipes = remember { mutableStateListOf<GeneratedRecipe>() }
    var email by remember { mutableStateOf("") }
    // Define categories and itemCounts at the AppNavigation level for persistence
    // Define categories mapped to ShoppingListItem
    val categories = remember {
        mutableStateMapOf(
            "Bakery" to mutableStateListOf<ShoppingListItem>(),
            "Vegetables" to mutableStateListOf<ShoppingListItem>(),
            "Fruits" to mutableStateListOf<ShoppingListItem>(),
            "Meat" to mutableStateListOf<ShoppingListItem>(),
            "Dairy" to mutableStateListOf<ShoppingListItem>(),
            "Other" to mutableStateListOf<ShoppingListItem>()
        )
    }

    // Define a state for user recipes
    var userRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }


    // Fetch user recipes when AppNavigation is first composed
    LaunchedEffect(Unit) {
        val token = AuthPreferences.getToken(context) ?: ""
        if (token.isEmpty()) {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }
    // Track item counts for each item globally
// Define addIngredientsToShoppingList with repository interaction
    val addIngredientsToShoppingList: (List<String>) -> Unit = { ingredients ->
        ingredients.forEach { ingredient ->
            val category = categorizeItem(ingredient)
            // Create ShoppingListItem with unique ID
            val newItem = ShoppingListItem(
                id = System.currentTimeMillis(), // Consider using UUID for uniqueness
                name = ingredient,
                category = category,
                count = 1
            )
            // Add to categories map
            categories.getOrPut(category) { mutableStateListOf() }.add(newItem)
            // Add to repository
            shoppingRepository.addItem(newItem) { addedItem ->
                if (addedItem != null) {
                    // Optionally, update the categories map if the backend assigns new IDs or other properties
                    // Here, assuming it returns the added item with the correct properties
                    categories.getOrPut(addedItem.category) { mutableStateListOf() }.remove(newItem)
                    categories.getOrPut(addedItem.category) { mutableStateListOf() }.add(addedItem)
                } else {
                    Toast.makeText(context, "Failed to add item: $ingredient", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val onAddClick: () -> Unit = {
        navController.navigate("addRecipeModal")
    }

    val onClose: () -> Unit = {
        navController.popBackStack()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // <--- Black behind NavHost
    ) {
    NavHost(navController = navController, startDestination = "onboarding") {
        composable("onboarding") { OnBoardingScreen(navController, context) }
        composable("splash") { SplashScreen(navController, context) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegistrationScreen(navController) }

        // Standard composable for "home"
        composable("home") {
            HomeScreen(navController, onAddClick = onAddClick, recipes = recipes)
        }

        composable("profile") {
            val context = LocalContext.current
            ProfileScreen(navController, context, recipes)
        }
        // Inside your NavHost in AppNavigation:
        composable("challenges") {
            CookingChallengesScreen(
                navController = navController,
                onChallengeClick = { challenge ->
                    navController.navigate("challengeDetail/${challenge.id}")
                },
                onSubmitChallenge = {

                    val userRole = AuthPreferences.getUserRole(context) ?: "user"
                    Log.d("NAVIGATION_DEBUG", "User role for navigation: $userRole")
                    if (userRole.equals("ADMIN", ignoreCase = true)) {
                        navController.navigate("submitFeaturedChallenge")
                    } else {
                        navController.navigate("submitChallenge")
                    }
                }, recipes = recipes
            )
        }
        composable("submitChallenge") {
            SubmitChallengeScreen(navController = navController)
        }
        composable("submitFeaturedChallenge") {
            SubmitFeaturedChallengeScreen(navController = navController)
        }

        composable("editProfile") {
            EditProfileScreen(
                navController = navController,
                context = context,
                onSave = { newUsername, newPassword ->
                    updateUserProfile(
                        context = context,
                        email = email,
                        username = newUsername,
                        password = newPassword,
                        onSuccess = {
                            navController.popBackStack()
                            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = {
                            Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                recipes = recipes
            )
        }

        composable(
            route = "favorites",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                )
            }
        ) {
            // Also ensure a black Box here
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                FavoritesScreen(navController, context, recipes)
            }
        }

        composable("authorDetail/{authorId}") { backStackEntry ->
            val authorId = backStackEntry.arguments?.getString("authorId")?.toLongOrNull()
            authorId?.let {
                AuthorDetailScreen(authorId = it, navController = navController, recipes = recipes)
            }
        }
        composable(
            route = "userSearch",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                )
            }
        ) {
            UserSearchScreen(navController = navController, recipes = recipes)
        }

        // Shopping Screen
        composable("shopping") {
            val context = LocalContext.current
            // Initialize MealPlanRepository using remember to preserve state across recompositions
            val mealPlanRepository = remember { MealPlanRepository(context) }

            ShoppingScreen(
                navController = navController,
                categories = categories,
                onAddIngredients = addIngredientsToShoppingList,
                recipes = recipes,
                shoppingRepository = shoppingRepository,
                mealPlanRepository = mealPlanRepository // Pass the MealPlanRepository
            )
        }
        composable("addRecipeModal") {
            // For example, you might have a variable or a state that holds the current subscription type.
            val currentSubscriptionType = "FREE" // or "PLUS" / "PRO" as appropriate
            AddRecipeBottomSheetContent(
                navController = navController,
                onClose = { navController.popBackStack() },
                recipes = recipes,
                subscriptionType = currentSubscriptionType
            )
        }
        composable("forgotPassword") { ForgotPasswordScreen(navController) }

        composable("premium") {
            PremiumScreen(navController, recipes, stripeManager)
        }


        composable("cookbookDetail/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")?.toLongOrNull()
            if (bookId != null) {
                CookbookDetailScreen(
                    navController = navController,
                    bookId = bookId,
                    recipes = recipes/* pass a list or fetch it inside the screen */
                )
            }
        }
        // Cookbooks Overview Screen
        composable("cookbooks") {
            CookbooksOverviewScreen(navController, recipes, subscriptionType = "FREE")
        }

        // **Updated "generateRecipe" Composable**
        composable("generateRecipe") {
            GenerateRecipeScreen(
                navController = navController,
                generatedRecipes = generatedRecipes,
                recipes = recipes,
                onAddRecipe = { newRecipe ->
                    if (generatedRecipes.none {
                            it.recipe.title.equals(
                                newRecipe.title,
                                ignoreCase = true
                            )
                        }) {
                        generatedRecipes.add(GeneratedRecipe(newRecipe))
                    } else {
                        // Handle duplicates by attempting to generate another recipe
                        // We'll implement retry logic within GenerateRecipeScreen
                        // For now, notify the user
                        Toast.makeText(
                            context,
                            "Duplicate recipe generated. Attempting to generate another...",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Trigger a re-generation by invoking a function within GenerateRecipeScreen
                        // This requires GenerateRecipeScreen to expose such functionality
                        // Alternatively, implement the retry logic inside GenerateRecipeScreen
                    }
                },
                onRemoveRecipe = { generatedRecipe ->
                    generatedRecipes.remove(generatedRecipe)
                },
                onToggleExpand = { generatedRecipe ->
                    generatedRecipe.isExpanded.value = !generatedRecipe.isExpanded.value
                }
            )
        }

        // Edit Recipe by ID
        composable(
            route = "editRecipe/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("id")
            EditRecipeScreen(
                navController = navController,
                recipeId = recipeId,
                recipes = recipes
            )
        }

        composable(
            route = "createRecipe?title={title}",
            arguments = listOf(
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            CreateRecipeScreen(
                navController = navController,
                initialTitle = title,
                recipes = recipes
            )
        }

        // Add a destination for the challenge detail screen:
        composable(
            route = "challengeDetail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            ChallengeDetailScreen(challengeId = id, navController = navController)
        }
        composable("discover") {
            DiscoverScreen(navController, onAddClick = onAddClick, recipes = recipes)
        }

        // Recipe Detail Screen
        composable(
            route = "recipeDetail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("id") ?: 0L
            RecipeDetailScreen(
                recipeId = recipeId,
                navController = navController,
                recipes = recipes,
                shoppingRepository = shoppingRepository, // Pass the repository
                onAddIngredientsToShoppingList = addIngredientsToShoppingList // Pass the callback
            )
        }
    }
        // Ensure no duplicate routes
    }
}