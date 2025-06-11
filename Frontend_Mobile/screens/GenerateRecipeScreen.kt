package com.example.recipeapp.screens

import android.os.Build.VERSION.SDK_INT
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.network.RecipeService
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.GeneratedRecipe
import com.example.recipeapp.utils.IngredientsLoader
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO
import com.example.recipeapp.utils.levenshteinDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GenerateRecipeScreen(
    navController: NavController,
    generatedRecipes: SnapshotStateList<GeneratedRecipe>,
    onAddRecipe: (Recipe) -> Unit,
    onRemoveRecipe: (GeneratedRecipe) -> Unit,
    onToggleExpand: (GeneratedRecipe) -> Unit,
    recipes: MutableList<Recipe>,
) {
    // Initialize RecipeRepository inside the composable
    val context = LocalContext.current
    val recipeService =
        remember { ApiClient.getRetrofit(context).create(RecipeService::class.java) }
    val recipeRepository = remember { RecipeRepository(context) }
    val username = AuthPreferences.getUsername(context) ?: ""
    var author by remember { mutableStateOf(username) }
    val defaultImage =
        "https://www.shutterstock.com/image-vector/illustration-cooking-chef-robot-character-600nw-2079445081.jpg"

    var singleIngredient by remember { mutableStateOf("") }
    var ingredientsList by remember { mutableStateOf(listOf<String>()) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var unrecognizedIngredients by remember { mutableStateOf(listOf<Pair<String, String?>>()) } // Pair of (input, suggestion)

    val coroutineScope = rememberCoroutineScope()

    // Load known ingredients from test.json once
    val knownIngredients by remember {
        mutableStateOf(IngredientsLoader.loadKnownIngredients(context))
    }

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
    fun findClosestIngredient(input: String): String? {
        val threshold = 3 // Maximum acceptable distance
        var closest: String? = null
        var minDistance = Int.MAX_VALUE
        for (known in knownIngredients) {
            val distance = levenshteinDistance(input, known)
            if (distance < minDistance && distance <= threshold) {
                minDistance = distance
                closest = known
            }
        }
        return closest
    }

    fun validateIngredients(ingredients: List<String>): Boolean {
        val cleaned = ingredients.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        var allValid = true
        val tempUnrecognized = mutableListOf<Pair<String, String?>>()

        for (ingredient in cleaned) {
            if (!knownIngredients.contains(ingredient)) {
                allValid = false
                val suggestion = findClosestIngredient(ingredient)
                tempUnrecognized.add(Pair(ingredient, suggestion))
            }
        }

        unrecognizedIngredients = tempUnrecognized
        return allValid
    }

    val scrollState = rememberScrollState()

    // Bottom sheet state if needed
    var isSheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Show bottom sheet if needed
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

    // Manage selectedTab for BottomNavigationBar consistency
    var selectedTab by remember { mutableStateOf("home") }

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
                            Text("LeGourmand", fontSize = 20.sp, color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        // Add the settings icon
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
                        containerColor = Color(0xFF211312),
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
                onAddClick = { isSheetVisible = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Background Image
            Image(
                painter = painterResource(id = R.drawable.dsss), // Replace with your image resource
                contentDescription = "Background image",
                modifier = Modifier.fillMaxSize().blur(4.dp),
                contentScale = ContentScale.Crop,
            )

            // Your card overlay
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent  // Make the Card itself transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4D4D4D).copy(alpha = 0.8f),
                                    Color(0xFF161616).copy(alpha = 0.8f)
                                )
                            )
                        )
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Let's cook something delicious with AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "What would you like to cook ?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = singleIngredient,
                            onValueChange = { singleIngredient = it },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { /* handle IME if needed */ }),
                            decorationBox = { innerTextField ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_shopping), // Replace with your icon resource
                                        contentDescription = "Ingredient Icon",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box {
                                        if (singleIngredient.isEmpty()) {
                                            Text(
                                                text = "Enter ingredients one by one...",  // Placeholder text
                                                color = Color.Gray,
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val trimmed = singleIngredient.trim()
                                if (trimmed.isNotEmpty()) {
                                    ingredientsList = ingredientsList + trimmed
                                    singleIngredient = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF876232)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Ingredient",
                                tint = Color.White
                            )
                        }
                    }

                    if (ingredientsList.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Your Ingredients:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                ingredientsList.forEach { ingredient ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Bullet Point
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .padding(top = 6.dp)
                                                .background(Color(0xFF00897B), shape = CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Ingredient Text
                                        Text(
                                            text = ingredient,
                                            color = Color(0xFFD3D2D2),
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Remove Icon
                                        IconButton(
                                            onClick = {
                                                ingredientsList = ingredientsList - ingredient
                                            },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove ingredient",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }

                    Button(
                        onClick = {
                            // Validate ingredients first…
                            val valid = validateIngredients(ingredientsList)
                            if (!valid) {
                                errorMessage = if (unrecognizedIngredients.isNotEmpty())
                                    "Some ingredients are not recognized. Please correct them."
                                else "Unknown error occurred."
                                return@Button
                            }

                            // Before calling generateRecipeFromAI, check the backend limit:
                            isLoading = true
                            errorMessage = null
                            unrecognizedIngredients = emptyList()

                            recipeRepository.checkGenerationLimit { allowed, checkError ->
                                if (!allowed) {
                                    isLoading = false
                                    errorMessage = checkError
                                        ?: "Monthly generation limit reached for your subscription tier."
                                    return@checkGenerationLimit
                                } else {
                                    val input = ingredientsList.joinToString(", ")
                                    // Call your existing AI generation method:
                                    recipeRepository.generateRecipeFromAI(input) { recipe ->
                                        coroutineScope.launch {
                                            withContext(Dispatchers.Main) {
                                                if (recipe != null) {
                                                    val isUnique = generatedRecipes.none {
                                                        it.recipe.title.equals(
                                                            recipe.title,
                                                            ignoreCase = true
                                                        )
                                                    }
                                                    if (isUnique) {
                                                        Log.d(
                                                            "GenerateRecipeScreen",
                                                            "Generated Recipe Title: ${recipe.title}"
                                                        )
                                                        onAddRecipe(recipe)
                                                        // Now record the generation event on the backend:
                                                        recipeRepository.recordGenerationEvent { success, recordError ->
                                                            if (!success) {
                                                                // Optionally show an error or log it (the recipe was still generated)
                                                                Log.e(
                                                                    "GenerateRecipeScreen",
                                                                    "Failed to record generation event: $recordError"
                                                                )
                                                            }
                                                        }
                                                    }
                                                    isLoading = false
                                                } else {
                                                    isLoading = false
                                                    errorMessage =
                                                        "Failed to generate recipe. Please try again."
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF866232)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {


                        Text(
                            text = "Generate Recipe",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                    }

                    if (isLoading) {
                        Cookgifs()
                    }

                    errorMessage?.let { message ->
                        if (unrecognizedIngredients.isNotEmpty()) {
                            // Display suggestions for unrecognized ingredients
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = message,
                                    color = Color.Red,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                unrecognizedIngredients.forEach { (input, suggestion) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Unrecognized: $input",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (suggestion != null) {
                                            Button(
                                                onClick = {
                                                    // Replace the unrecognized ingredient with the suggestion
                                                    ingredientsList = ingredientsList.map {
                                                        if (it.equals(
                                                                input,
                                                                ignoreCase = true
                                                            )
                                                        ) suggestion else it
                                                    }
                                                    unrecognizedIngredients =
                                                        unrecognizedIngredients.filter { it.first != input }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF00897B)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Text(
                                                    "Replace with \"$suggestion\"",
                                                    color = Color.White,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "No suggestion available",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        } else {
                            // General error message
                            Text(
                                text = message,
                                color = Color.Red,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Display the list of generated recipes
                    generatedRecipes.forEach { generatedRecipe ->
                        RecipeDisplayCard(
                            generatedRecipe = generatedRecipe,
                            onAddToRecipes = { recipe ->
                                // Handle adding to user's recipes
                                val userRecipe = recipe.copy(
                                    title = recipe.title ?: "Untitled",
                                    author = author,
                                    imageUri = defaultImage,
                                    servings = recipe.servings ?: "1",
                                    prepTime = recipe.prepTime ?: "",
                                    cookTime = recipe.cookTime ?: "",
                                    ingredients = recipe.ingredients ?: "",
                                    instructions = recipe.instructions ?: "",
                                    notes = recipe.notes ?: "",
                                    url = recipe.url ?: "",
                                    tags = recipe.tags ?: emptyList(),
                                    difficulty = recipe.difficulty ?: "",
                                    cuisine = recipe.cuisine ?: "",
                                    source = recipe.source ?: "",
                                    video = recipe.video ?: "",
                                    calories = recipe.calories ?: "",
                                    carbohydrates = recipe.carbohydrates ?: "",
                                    protein = recipe.protein ?: "",
                                    fat = recipe.fat ?: "",
                                    sugar = recipe.sugar ?: "",
                                    isPublic = false
                                )

                                recipeRepository.createRecipe(userRecipe) { savedRecipe ->
                                    coroutineScope.launch {
                                        withContext(Dispatchers.Main) {
                                            if (savedRecipe != null) {
                                                Toast.makeText(
                                                    context,
                                                    "Recipe added successfully!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to add recipe. Try again.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            },
                            onRemove = {
                                onRemoveRecipe(generatedRecipe)
                            },
                            onToggleExpand = {
                                onToggleExpand(generatedRecipe)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                    }
                    if (!isLoading) {
                        AnimatedGif()
                    }
                }
            }
        }
    }
}}
@Composable
fun RecipeDisplayCard(
    generatedRecipe: GeneratedRecipe,
    onAddToRecipes: (Recipe) -> Unit,
    onRemove: () -> Unit,
    onToggleExpand: (GeneratedRecipe) -> Unit
) {
    val recipe = generatedRecipe.recipe

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF302F2F)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row with Title and Expand/Collapse Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = generatedRecipe.recipe.title ?: "No Title",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onToggleExpand(generatedRecipe) }) {
                    Icon(
                        imageVector = if (generatedRecipe.isExpanded.value) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (generatedRecipe.isExpanded.value) "Collapse" else "Expand",
                        tint = Color.White
                    )
                }
            }

            // Expanded Content
            if (generatedRecipe.isExpanded.value) {
                Spacer(modifier = Modifier.height(12.dp))

                // Ingredients Section
                Text(
                    text = "Ingredients:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    // Split ingredients based on commas or newlines
                    val ingredients = generatedRecipe.recipe.ingredients?.split(",")?.map { it.trim() } ?: emptyList()
                    Log.d("RecipeDisplayCard", "Ingredients: $ingredients")
                    ingredients.forEach { ingredient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Bullet Point
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .padding(top = 6.dp)
                                    .background(Color(0xFF00897B), shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Ingredient Text
                            Text(
                                text = ingredient,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }


                // Instructions Section
                Text(
                    text = "Instructions:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    // Split instructions based on newlines
                    val instructions = generatedRecipe.recipe.instructions
                        ?.split(Regex("(?=\\d+\\.)"))  // Split where a digit+dot appears, e.g. "1."
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()
                    Log.d("RecipeDisplayCard", "Instructions: $instructions")
                    instructions.forEach { instruction ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp) // Optional: Adds some vertical spacing between instructions
                        ) {
                            // Instruction Text
                            Text(
                                text = instruction,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                }

                Spacer(modifier = Modifier.height(12.dp))

                // Servings, Prep Time, Cook Time with section headers
                if (!generatedRecipe.recipe.servings.isNullOrEmpty()) {
                    Text("Servings:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(generatedRecipe.recipe.servings, color = Color.Gray, fontSize = 14.sp)
                }

                if (!generatedRecipe.recipe.prepTime.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Prep Time (min):", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(generatedRecipe.recipe.prepTime, color = Color.Gray, fontSize = 14.sp)
                }

                if (!generatedRecipe.recipe.cookTime.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cook Time (min):", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(generatedRecipe.recipe.cookTime, color = Color.Gray, fontSize = 14.sp)
                }

                // Notes
                if (!generatedRecipe.recipe.notes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Notes:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(generatedRecipe.recipe.notes, color = Color.Gray, fontSize = 14.sp)
                }

                // Difficulty, Cuisine
                if (!generatedRecipe.recipe.difficulty.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Difficulty:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(generatedRecipe.recipe.difficulty, color = Color.Gray, fontSize = 14.sp)
                }

                if (!generatedRecipe.recipe.cuisine.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cuisine:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(generatedRecipe.recipe.cuisine, color = Color.Gray, fontSize = 14.sp)
                }

                // Source
                if (!generatedRecipe.recipe.source.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Source:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(generatedRecipe.recipe.source, color = Color.Gray, fontSize = 14.sp)
                }

                // Video
                if (!generatedRecipe.recipe.video.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Video:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(generatedRecipe.recipe.video, color = Color.Gray, fontSize = 14.sp)
                }

                // Nutrients
                val anyNutrient = listOf(
                    generatedRecipe.recipe.calories,
                    generatedRecipe.recipe.carbohydrates,
                    generatedRecipe.recipe.protein,
                    generatedRecipe.recipe.fat,
                    generatedRecipe.recipe.sugar
                ).any { !it.isNullOrEmpty() }
                if (anyNutrient) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nutrients:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)

                    if (!generatedRecipe.recipe.calories.isNullOrEmpty()) {
                        Text("Calories (kcal): ${generatedRecipe.recipe.calories}", color = Color.Gray, fontSize = 14.sp)
                    }
                    if (!generatedRecipe.recipe.carbohydrates.isNullOrEmpty()) {
                        Text("Carbohydrates (g): ${generatedRecipe.recipe.carbohydrates}", color = Color.Gray, fontSize = 14.sp)
                    }
                    if (!generatedRecipe.recipe.protein.isNullOrEmpty()) {
                        Text("Protein (g): ${generatedRecipe.recipe.protein}", color = Color.Gray, fontSize = 14.sp)
                    }
                    if (!generatedRecipe.recipe.fat.isNullOrEmpty()) {
                        Text("Fat (g): ${generatedRecipe.recipe.fat}", color = Color.Gray, fontSize = 14.sp)
                    }
                    if (!generatedRecipe.recipe.sugar.isNullOrEmpty()) {
                        Text("Sugar (g): ${generatedRecipe.recipe.sugar}", color = Color.Gray, fontSize = 14.sp)
                    }
                }

                // Tags
                val tags = generatedRecipe.recipe.tags ?: emptyList()
                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tags:",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(tags) { tag ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF00897B), shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Public: ${if (generatedRecipe.recipe.isPublic == true) "Yes" else "No"}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                if (recipe.isAiGenerated) {
                    Text(
                        text = """
                        Generated by the LeGourmand AI.
                        This recipe is AI-generated and LeGourmand has not reviewed it for accuracy or safety.
                        Use your best judgment when preparing AI-generated dishes.
                        Please rate this recipe to help others know if it's good or not.
                    """.trimIndent(),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Buttons: Add to Recipes and Remove
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { onAddToRecipes(generatedRecipe.recipe) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add to Recipes", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onRemove() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                }
            }
        }
    }
}
@Composable
fun AnimatedGif() {
    val context = LocalContext.current

    // Optionally create an ImageLoader that explicitly supports GIF decoding:
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    // If you have a local GIF resource, reference it here:
    val painter = rememberAsyncImagePainter(
        model = R.drawable.roro,  // Your GIF resource
        imageLoader = imageLoader
    )

    Image(
        painter = painter,
        contentDescription = "Animated GIF",
        modifier = Modifier.size(200.dp) // Adjust as needed
    )
}
@Composable
fun Cookgifs() {
    val context = LocalContext.current

    // Optionally create an ImageLoader that explicitly supports GIF decoding:
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    // If you have a local GIF resource, reference it here:
    val painter = rememberAsyncImagePainter(
        model = R.drawable.azr,  // Your GIF resource
        imageLoader = imageLoader
    )

    Image(
        painter = painter,
        contentDescription = "Animated GIF",
        modifier = Modifier.size(200.dp) // Adjust as needed
    )
}