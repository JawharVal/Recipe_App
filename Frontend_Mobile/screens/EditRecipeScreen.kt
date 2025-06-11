package com.example.recipeapp.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.components.CustomOutlinedDropdownField
import com.example.recipeapp.components.CustomOutlinedTextField
import com.example.recipeapp.components.DeleteRecipeButtonWithConfirmation
import com.example.recipeapp.components.ImagePickerDialog
import com.example.recipeapp.components.NutrientField
import com.example.recipeapp.components.SectionCard
import com.example.recipeapp.components.SectionTitle
import com.example.recipeapp.components.TagChip
import com.example.recipeapp.components.TagDialog
import com.example.recipeapp.network.RecipeService
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.ChallengeApi
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UploadImageUtil
import com.example.recipeapp.utils.UploadRecipeImageUtil
import com.example.recipeapp.utils.UserDTO
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecipeScreen(

    navController: NavController,
    recipeId: Long? = null, // Used for editing; null for creating a new recipe
    initialTitle: String = "",
    initialServings: String = "",
    initialIngredients: String = "",
    initialAuthor: String = "CJ",
    initialUrl: String = "",
    initialImageUri: Uri? = null,
    initialTags: List<String> = emptyList(),
    initialInstructions: String = "",
    initialNotes: String = "",
    initialPrepTime: String = "",
    initialCookTime: String = "",
    initialCalories: String = "",
    initialCarbohydrates: String = "",
    initialProtein: String = "",
    initialFat: String = "",
    initialSugar: String = "",
    initialLanguage: String = "English",
    initialSource: String = "",
    initialVideo: String = "",
    initialDifficulty: String = "Not set",
    initialCuisine: String = "Not set",
    initialIsPublic: Boolean = true,
    recipes: MutableList<Recipe>
) {
    // Retrofit and repository initialization
    val context = LocalContext.current
    val username = AuthPreferences.getUsername(context) ?: ""
    Log.d("GenerateRecipeScreen", "Username: $username")
    // Retrofit and repository initialization
    val recipeService = ApiClient.getRetrofit(context).create(RecipeService::class.java)
    val recipeRepository = RecipeRepository(context)
    // State for recipe fields
    var title by remember { mutableStateOf(initialTitle) }

    var servings by remember { mutableStateOf(initialServings) }
    var author by remember { mutableStateOf(username) }
    var ingredients by remember { mutableStateOf(initialIngredients) }
    var instructions by remember { mutableStateOf(initialInstructions) }
    var notes by remember { mutableStateOf(initialNotes) }
    var preparationTime by remember { mutableStateOf(initialPrepTime) }
    var cookTime by remember { mutableStateOf(initialCookTime) }
    var calories by remember { mutableStateOf(initialCalories) }
    var carbohydrates by remember { mutableStateOf(initialCarbohydrates) }
    var protein by remember { mutableStateOf(initialProtein) }
    var fat by remember { mutableStateOf(initialFat) }
    var sugar by remember { mutableStateOf(initialSugar) }
    var tags by remember { mutableStateOf(initialTags) }
    var difficulty by remember { mutableStateOf(initialDifficulty) }
    var cuisine by remember { mutableStateOf(initialCuisine) }
    var source by remember { mutableStateOf(initialSource) }
    var video by remember { mutableStateOf(initialVideo) }
    var language by remember { mutableStateOf(initialLanguage) }
    var isPublic by remember { mutableStateOf(initialIsPublic) }
    var selectedImageUri by remember {
        mutableStateOf(initialImageUri ?: Uri.parse("android.resource://com.example.recipeapp/${R.drawable.aaz}"))
    }

    // Error states and validation
    var shouldShowErrors by remember { mutableStateOf(false) }
    val isCookTimeValid = cookTime.isEmpty() || cookTime.toIntOrNull() != null
    val isVideoUrlValid = video.isEmpty() || android.util.Patterns.WEB_URL.matcher(video).matches()
    val isSourceUrlValid = source.isEmpty() || android.util.Patterns.WEB_URL.matcher(source).matches()

    val cookTimeError = if (!isCookTimeValid) "Please enter a valid number" else null
    val videoError = if (!isVideoUrlValid) "Please enter a valid URL" else null
    val sourceError = if (!isSourceUrlValid) "Please enter a valid URL" else null

    var url by remember { mutableStateOf("") }

    var showImagePickerDialog by remember { mutableStateOf(false) }
    // State for Tag Dialog
    var showTagDialog by remember { mutableStateOf(false) }
    var tagSearch by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var preparationTimeError by remember { mutableStateOf<String?>(null) }

    var caloriesError by remember { mutableStateOf<String?>(null) }
    var carbohydratesError by remember { mutableStateOf<String?>(null) }
    var proteinError by remember { mutableStateOf<String?>(null) }
    var fatError by remember { mutableStateOf<String?>(null) }
    var sugarError by remember { mutableStateOf<String?>(null) }
    var isSheetVisible by remember { mutableStateOf(false) }

    val languageOptions = listOf("Russian", "English", "Spanish", "French", "German", "Chinese")
    val difficultyOptions = listOf("Easy", "Medium", "Hard")
    val cuisineOptions = listOf("Russian",
        "Italian",
        "Mexican",
        "Indian",
        "Chinese",
        "Japanese",
        "American",
        "French",
        "Arabian",
        "Thai",
        "Spanish",
        "Mediterranean",
        "Korean",
        "Vietnamese")
    var titleError by remember { mutableStateOf<String?>(null) }


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
    // Fetch recipe details if editing
    LaunchedEffect(recipeId) {
        recipeId?.let {
            recipeRepository.getRecipeById(it) { recipe ->
                recipe?.let {
                    title = it.title
                    servings = it.servings
                    author = it.author
                    ingredients = it.ingredients
                    instructions = it.instructions
                    notes = it.notes
                    preparationTime = it.prepTime
                    cookTime = it.cookTime
                    calories = it.calories
                    carbohydrates = it.carbohydrates
                    protein = it.protein
                    fat = it.fat
                    sugar = it.sugar
                    tags = it.tags
                    difficulty = it.difficulty
                    cuisine = it.cuisine
                    source = it.source
                    video = it.video
                    isPublic = it.isPublic
                    selectedImageUri = it.imageUri?.let { uri -> Uri.parse(uri) }
                }
            }
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
                                tint = Color(0xFF886332)
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
                        containerColor = Color(0xFF252322),
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
        floatingActionButton = {
            Button(
                onClick = {
                    var foundAnyBadWords = false

                    val (newTitle, titleHasBadWords) = censorAndCheck(title)
                    title = newTitle
                    foundAnyBadWords = foundAnyBadWords || titleHasBadWords

                    val (newIngredients, ingredientsHasBadWords) = censorAndCheck(ingredients)
                    ingredients = newIngredients
                    foundAnyBadWords = foundAnyBadWords || ingredientsHasBadWords

                    val (newInstructions, instructionsHasBadWords) = censorAndCheck(instructions)
                    instructions = newInstructions
                    foundAnyBadWords = foundAnyBadWords || instructionsHasBadWords

                    val (newNotes, notesHasBadWords) = censorAndCheck(notes)
                    notes = newNotes
                    foundAnyBadWords = foundAnyBadWords || notesHasBadWords
                    val censoredTags = censorTagList(tags)
                    tags = censoredTags

                    val (newVideo, videoHasBadWords) = censorAndCheck(video)
                    video = newVideo
                    foundAnyBadWords = foundAnyBadWords || videoHasBadWords

                    val (newSource, sourceHasBadWords) = censorAndCheck(source)
                    source = newSource
                    foundAnyBadWords = foundAnyBadWords || sourceHasBadWords

                    // 3) Check for banned domains in video and source
                    val bannedDomains = listOf(
                        "badwebsite.com",
                        "malicious.org",
                        "scamsite.com",
                        "adultcontent.com",
                        "xxx.com"
                    )
                    val domainFlag = bannedDomains.any { banned ->
                        source.contains(banned, ignoreCase = true) || video.contains(banned, ignoreCase = true)
                    }

                    if (videoHasBadWords || sourceHasBadWords || domainFlag) {
                        foundAnyBadWords = true
                    }
// Define your banned domains list (if needed)
                    // Define a list of banned domains.



// Combine all flags
                    val anyBadContent = videoHasBadWords || sourceHasBadWords || domainFlag

                    if (anyBadContent) {
                        isPublic = false
                        Toast.makeText(
                            context,
                            "Cannot set recipe public due to inappropriate video or source URL. Please check your inputs.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    // If we found any profanity, forcibly set isPublic = false
                    if (foundAnyBadWords) {
                        isPublic = false
                        Toast.makeText(
                            context,
                            "Profanity detected. Your recipe is set to private until cleaned up.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    val recipeExists = recipeId == null && recipes.any { it.title.equals(title, ignoreCase = true) }
                    val allFieldsValid = title.trim().isNotEmpty() &&
                            isCookTimeValid && isVideoUrlValid && isSourceUrlValid

                    if (!recipeExists && allFieldsValid) {
                        val newRecipe = Recipe(
                            title = title,
                            author = author,
                            prepTime = preparationTime,
                            cookTime = cookTime,
                            servings = servings,
                            ingredients = ingredients,
                            instructions = instructions,
                            notes = notes,
                            url = "",
                            imageUri = selectedImageUri?.toString(),
                            tags = tags,
                            difficulty = difficulty,
                            cuisine = cuisine,
                            source = source,
                            video = video,
                            calories = calories,
                            carbohydrates = carbohydrates,
                            protein = protein,
                            fat = fat,
                            sugar = sugar,
                            isPublic = isPublic,
                        )

                        if (recipeId == null) {
                            // Creating a new recipe
                            Log.d("EditRecipeScreen", "Creating a new recipe: $newRecipe")
                            recipeRepository.createRecipe(newRecipe) { createdRecipe ->
                                if (createdRecipe != null) {
                                    Log.d("EditRecipeScreen", "Recipe created successfully: $createdRecipe")
                                    Toast.makeText(context, "Recipe created successfully.", Toast.LENGTH_SHORT).show()
                                    // Optionally, navigate to the edit screen with the new ID
                                    navController.navigate("editRecipe/${createdRecipe.id}") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                } else {
                                    Log.e("EditRecipeScreen", "Failed to create recipe.")
                                    Toast.makeText(context, "Failed to save recipe.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // Updating an existing recipe
                            Log.d("EditRecipeScreen", "Updating recipe ID $recipeId with data: $newRecipe")
                            recipeRepository.updateRecipe(recipeId, newRecipe) { updatedRecipe ->
                                if (updatedRecipe != null) {
                                    Log.d("EditRecipeScreen", "Recipe updated successfully: $updatedRecipe")
                                    Toast.makeText(context, "Recipe updated successfully.", Toast.LENGTH_SHORT).show()
                                    navController.navigate("home") {
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    }
                                } else {
                                    Log.e("EditRecipeScreen", "Failed to update recipe.")
                                    Toast.makeText(context, "Failed to update recipe.", Toast.LENGTH_SHORT).show()
                                }
                            }


                        }
                    } else {
                        shouldShowErrors = true
                        titleError = when {
                            title.trim().isEmpty() -> "Title is required"
                            recipeExists -> "A recipe with this title already exists"
                            else -> null
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00897B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save", fontSize = 16.sp)
            }
        }




    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF292828))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                // General Section
                Text(
                    text = "Edit recipe",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                // General Section with Description
                SectionCard(
                    backgroundColor = Color(0xFF1C1917),
                    borderColor = Color.Gray,
                    borderWidth = 1.dp
                ) {
                    SectionTitle("General")
                    Text(
                        text = "Only the title is required. You can edit the recipe at any time.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Title", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = if (title.trim().isEmpty()) "Title is required" else null
                        },
                        label = "Title",
                        shouldShowError = titleError != null,
                        errorMessage = titleError
                    )

                    Spacer(modifier = Modifier.height(23.dp))

                    Text("Tags", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            TagChip(tag, onRemove = { tags = tags - tag })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showTagDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF876332), // Green color for text and icon
                            containerColor = Color.Transparent // Keeps the background transparent
                        ),
                        border = BorderStroke(4.dp, Color(0xFF876332)), // Brown border color
                        shape = RoundedCornerShape(18.dp) // Green border color
                    ) {
                        Text("+ Add tag")
                    }
                }

                // Image Section with Description
                SectionCard(
                    backgroundColor = Color(0xFF1C1917),
                    borderColor = Color.Gray,
                    borderWidth = 1.dp
                ) {
                    SectionTitle("Image")
                    Text(
                        text = "Add an image to make your recipe more appealing.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.Gray)
                            .clickable { showImagePickerDialog = true } // Opens image picker when clicked
                            .border(
                                width = 2.dp,
                                color = Color.White, // Customize the border color
                                shape = RoundedCornerShape(8.dp) // Adjust the shape as needed
                            )
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = selectedImageUri),
                            contentDescription = "Recipe Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Show close icon only if a custom image is selected
                        if (selectedImageUri.toString() != "android.resource://com.example.recipeapp/${R.drawable.aaz}") {
                            IconButton(
                                onClick = {
                                    selectedImageUri = Uri.parse("android.resource://com.example.recipeapp/${R.drawable.aaz}")
                                },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showImagePickerDialog = true },
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF876332), // Green color for text and icon
                            containerColor = Color.Transparent // Keeps the background transparent
                        ),
                        border = BorderStroke(4.dp, Color(0xFF876332)), // Brown border color
                        shape = RoundedCornerShape(18.dp) // Green border color
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Edit image")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit image")
                    }
                }

                // Content Section with Description
                SectionCard(
                    backgroundColor = Color(0xFF1C1917),
                    borderColor = Color.Gray,
                    borderWidth = 1.dp
                ) {
                    SectionTitle("Content")
                    Text(
                        text = "The actual recipe content.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Servings", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedTextField(
                        value = servings,
                        onValueChange = { servings = it },
                        label = "Servings",
                        shouldShowError = false,
                        errorMessage = null,
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    // Ingredients Field
                    Text("Ingredients", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedTextField(
                        value = ingredients,
                        onValueChange = { ingredients = it },
                        label = "Ingredients (Each ingredient = new line)",
                        shouldShowError = false,
                        errorMessage = null,
                        singleLine = false, // Allows multi-line input
                        maxLines = 15 // Adjust as needed
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Instructions", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = "Instructions (Each step = new line)",
                        shouldShowError = false,
                        errorMessage = null,
                        singleLine = false, // Allows multi-line input
                        maxLines = 10 // Adjust as needed for your design
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Notes", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes (Each note = new line)",
                        shouldShowError = false,
                        errorMessage = null,
                        singleLine = false, // Allows multi-line input
                        maxLines = 5 // Adjust as needed
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Preparation time", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedTextField(
                        value = preparationTime,
                        onValueChange = { preparationTime = it },
                        label = "Preparation time",
                        trailingText = "min",
                        shouldShowError = shouldShowErrors && preparationTime.toIntOrNull() == null,
                        errorMessage = "Please enter a valid number"
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Cook time", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedTextField(
                        value = cookTime,
                        onValueChange = { cookTime = it },
                        label = "Cook time",
                        trailingText = "min",
                        shouldShowError = shouldShowErrors && !isCookTimeValid,
                        errorMessage = cookTimeError
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nutrients Section with Description
                SectionCard(
                    backgroundColor = Color(0xFF1C1917),
                    borderColor = Color.Gray,
                    borderWidth = 1.dp
                ) {
                    SectionTitle("Nutrients")
                    Text(
                        text = "Enhance the recipe with nutritional information.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Calories", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    NutrientField(calories, { calories = it }, "Calories", "kcal", shouldShowErrors)
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Carbohydrates", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    NutrientField(carbohydrates, { carbohydrates = it }, "Carbohydrates", "g", shouldShowErrors)
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Protein", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    NutrientField(protein, { protein = it }, "Protein", "g", shouldShowErrors)
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Fat", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    NutrientField(fat, { fat = it }, "Fat", "g", shouldShowErrors)
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Sugar", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    NutrientField(sugar, { sugar = it }, "Sugar", "g", shouldShowErrors)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Extra Section with Description
                SectionCard(
                    backgroundColor = Color(0xFF1C1917),
                    borderColor = Color.Gray,
                    borderWidth = 1.dp
                ) {
                    SectionTitle("Extra")
                    Text(
                        text = "Additional information about the recipe.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Video", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedTextField(
                        value = video,
                        onValueChange = { video = it },
                        label = "Video",
                        shouldShowError = shouldShowErrors && !isVideoUrlValid,
                        errorMessage = videoError
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Source", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = "Source",
                        shouldShowError = shouldShowErrors && !isSourceUrlValid,
                        errorMessage = sourceError
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Difficulty", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedDropdownField(
                        value = difficulty,
                        onValueChange = { difficulty = it },
                        label = "Difficulty",
                        options = difficultyOptions
                    )
                    Spacer(modifier = Modifier.height(23.dp))
                    Text("Cuisine", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CustomOutlinedDropdownField(
                        value = cuisine,
                        onValueChange = { cuisine = it },
                        label = "Cuisine",
                        options = cuisineOptions
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Public",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { newValue ->
                                // The user is attempting to set isPublic to newValue
                                if (newValue) {
                                    // user wants public => check for profanity
                                    val (censoredTitle, titleBad) = censorAndCheck(title)
                                    val (censoredIngredients, ingBad) = censorAndCheck(ingredients)
                                    val (censoredInstructions, insBad) = censorAndCheck(instructions)
                                    val (censoredNotes, notesBad) = censorAndCheck(notes)

                                    val anyBadWords = titleBad || ingBad || insBad || notesBad
                                    if (anyBadWords) {
                                        // Force user to remain private
                                        isPublic = false
                                        Toast.makeText(
                                            context,
                                            "Cannot set recipe public due to profanity. Please remove bad words first.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        // It's clean => let them set public
                                        isPublic = true
                                    }
                                } else {
                                    // user wants private => just allow
                                    isPublic = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                // your switch colors
                            )
                        )
                    }

                    Text(
                        text = "By submitting a recipe, you agree that you have the right to publish the recipe image and content and to allow LeGourmand to store and distribute the recipe.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.End)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                DeleteRecipeButtonWithConfirmation(navController = navController)
            }
        }
    }
    // Show Tag Dialog when required
    if (showTagDialog) {
        TagDialog(
            tagSearch = tagSearch,
            onTagSearchChange = { tagSearch = it },
            onTagAdd = { newTag ->
                if (newTag.isNotBlank() && newTag !in tags) {
                    tags = tags + newTag
                }
                showTagDialog = false
                tagSearch = ""
            },
            onDismiss = { showTagDialog = false }
        )
    }

    if (showImagePickerDialog) {
        ImagePickerDialog(
            context = LocalContext.current,
            onDismiss = { showImagePickerDialog = false },
            onImageSelected = { uri ->
                if (uri == null) {
                    Log.e("ImagePicker", "❌ Selected URI is null!")
                    Toast.makeText(context, "Error: Selected image is invalid.", Toast.LENGTH_SHORT).show()
                    return@ImagePickerDialog
                }


                Log.d("ImagePicker", "✅ Selected Image Uri: $uri")


                coroutineScope.launch {
                    try {
                        // ✅ Upload the image to Appwrite as a recipe image (isAvatar = false)
                        UploadImageUtil.uploadImage(context, uri, isAvatar = false, onSuccess = { imageUrl ->
                            Log.d("ImagePicker", "📡 Uploaded Recipe Image URL: $imageUrl")

                            // ✅ Send URL to moderateImage API asynchronously
                            val paymentService = ApiClient.getRetrofit(context).create(ChallengeApi::class.java)
                            val call = paymentService.moderateImage(imageUrl)
                            call.enqueue(object : retrofit2.Callback<Map<String, Boolean>> {
                                override fun onResponse(
                                    call: retrofit2.Call<Map<String, Boolean>>,
                                    response: retrofit2.Response<Map<String, Boolean>>
                                ) {
                                    if (response.isSuccessful) {
                                        val body = response.body()
                                        val isAppropriate = body?.get("isAppropriate") ?: false
                                        Log.d("ImagePicker", "✅ Moderation API Result: $isAppropriate")

                                        if (!isAppropriate) {
                                            Toast.makeText(context, "❌ Image is inappropriate!", Toast.LENGTH_LONG).show()
                                            return
                                        }

                                        // ✅ Set Recipe Image URI
                                        selectedImageUri = Uri.parse(imageUrl)
                                        Toast.makeText(context, "✅ Recipe image selected successfully.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Log.e("ImagePicker", "❌ Moderation API Failed: ${response.errorBody()?.string()}")
                                        Toast.makeText(context, "Error moderating image.", Toast.LENGTH_LONG).show()
                                    }
                                }

                                override fun onFailure(
                                    call: retrofit2.Call<Map<String, Boolean>>,
                                    t: Throwable
                                ) {
                                    Log.e("ImagePicker", "❌ API Request Failed", t)
                                    Toast.makeText(context, "API Error: ${t.message}", Toast.LENGTH_LONG).show()
                                }
                            })
                        }, onError = { errorMsg ->
                            Log.e("ImageUpload", "❌ Image upload failed: $errorMsg")
                            Toast.makeText(context, "Upload failed: $errorMsg", Toast.LENGTH_SHORT).show()
                        })
                    } catch (e: Exception) {
                        Log.e("ImagePicker", "❌ Error processing image: ${e.message}")
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }


                // close the dialog
                showImagePickerDialog = false
            }
        )
    }


}



@Composable
fun EditRecipeTopBar(navController: NavController) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Arrow Icon
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Centered Logo and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter), // Replace with your logo resource
                    contentDescription = "Logo",
                    modifier = Modifier.size(32.dp), // Adjust size as needed
                    tint = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LeGourmand",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Settings Icon aligned to the right
            IconButton(onClick = { /* Handle settings click */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings), // Replace with your settings icon resource
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
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