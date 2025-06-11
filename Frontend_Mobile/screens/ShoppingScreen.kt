package com.example.recipeapp.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.navigation.NavController
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.components.BottomNavigationBar
import com.example.recipeapp.utils.AddShoppingItemSection
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.AuthPreferences
import com.example.recipeapp.utils.MealPlanRepository
import com.example.recipeapp.utils.Note
import com.example.recipeapp.utils.NoteDTO
import com.example.recipeapp.utils.ShoppingListItem
import com.example.recipeapp.utils.ShoppingRepository
import com.example.recipeapp.utils.UserDTO
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.DayOfWeek
import java.util.Locale
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    navController: NavController,
    categories: MutableMap<String, SnapshotStateList<ShoppingListItem>>,
    onAddIngredients: (List<String>) -> Unit,
    recipes: MutableList<Recipe>,

    shoppingRepository: ShoppingRepository,
    mealPlanRepository: MealPlanRepository // New parameter
) {
    val context = LocalContext.current

    // State variables for empty list input
    var emptyInputText by remember { mutableStateOf("") }
    var showAddOptionsModal by remember { mutableStateOf(false) }
    // State variables for adding items when list is not empty
    var addInputText by remember { mutableStateOf("") }

    var selectedTab by remember { mutableStateOf("shoppingList") }
    var sortAscending by remember { mutableStateOf(true) }
    var isSheetVisible by remember { mutableStateOf(false) }

    // Manage planned recipes and notes
    val plannedRecipes = remember { mutableStateMapOf<LocalDate, SnapshotStateList<Recipe>>() }
    val notes = remember { mutableStateMapOf<LocalDate, SnapshotStateList<Note>>() }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var plusUnlocked by remember { mutableStateOf(false) }
    var proUnlocked by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isShoppingLoading by remember { mutableStateOf(true) }
    var isMealPlanLoading by remember { mutableStateOf(true) }
    // A trigger that will re-run the data fetching when incremented.
    var retryTrigger by remember { mutableStateOf(0) }
    val authService = ApiClient.getAuthService(context)
    // Fetch profile (to set subscription flags)
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
                    Log.e("ShoppingScreen", "Failed to fetch profile: ${response.code()} ${response.message()}")
                    errorMessage = "Failed to load profile: ${response.message()}"
                }
            }

            override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                Log.e("ShoppingScreen", "Error fetching profile: ${t.localizedMessage ?: "Unknown error"}")
                errorMessage = "Failed to load profile. Please check your connection."
            }
        })
    }

    // ----------------- FETCH DATA (Shopping Items & Meal Plan) ----------------- //
    // Using retryTrigger as a key so that a change will re-run this block.
    LaunchedEffect(retryTrigger) {
        // Reset error state and loading flags
        errorMessage = null
        isShoppingLoading = true
        isMealPlanLoading = true

        // --- Fetch Shopping List Items ---
        shoppingRepository.getAllItems { items ->
            if (items != null) {
                // Clear current items to avoid duplication
                categories.values.forEach { it.clear() }
                items.forEach { item ->
                    val category = categorizeItem(item.name)
                    categories.getOrPut(category) { mutableStateListOf() }.add(item)
                }
            } else {
                errorMessage = "Failed to load shopping list."
            }
            isShoppingLoading = false
        }

        // --- Fetch Meal Plans for the Week ---
        val today = LocalDate.now()
        val currentStartOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val datesOfWeek = (0..6).map { offset -> currentStartOfWeek.plusDays(offset.toLong()) }
        var processedCount = 0

        datesOfWeek.forEach { date ->
            mealPlanRepository.getMealPlanForDate(date.toString()) { mealPlan ->
                processedCount++
                if (mealPlan != null) {
                    plannedRecipes[date] = mutableStateListOf()
                    mealPlan.recipes.forEach { recipeDTO ->
                        val recipe = recipes.find { it.id == recipeDTO.id }
                        recipe?.let { plannedRecipes[date]?.add(it) }
                    }
                    notes[date] = mutableStateListOf()
                    mealPlan.notes.forEach { noteDTO ->
                        notes[date]?.add(
                            Note(
                                id = noteDTO.id,
                                content = noteDTO.content
                            )
                        )
                    }
                } else {
                    errorMessage = "Failed to load shopping/meal content."}
                if (processedCount == datesOfWeek.size) {
                    isMealPlanLoading = false
                }
            }
        }
    }
    // ----------------- END FETCHING ----------------- //

    // Handle Add Options Modal and Bottom Sheet (unchanged)
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

    if (errorMessage != null) {
        ErrorPageDarkz(
            message = errorMessage!!,
            navController = navController,
            onRetry = { retryTrigger++ },
            onLogout = {
                AuthPreferences.clearToken(context)
                navController.navigate("login") {
                    popUpTo("shopping") { inclusive = true }
                }
            }
        )
    } else {
    // Scaffold and UI Implementation
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedTab = "shopping",
                onTabSelected = { /* Handle tab switching */ },
                onAddClick = { showAddOptionsModal = true }
            )
        }
    ) { innerPadding ->

            // Show a loading indicator while data is being fetched.
            if (isShoppingLoading || isMealPlanLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF876232))
                }
            } else {
                // Column with padding and background
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(innerPadding)

                ) {
                    TopBar(navController)
                    ShoppingScreenTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it })

                    if (selectedTab == "shoppingList") {
                        SortingAndFilterRow(
                            sortAscending = sortAscending,
                            onSortToggle = { sortAscending = !sortAscending }
                        )
                    }

                    when (selectedTab) {
                        "shoppingList" -> {
                            if (categories.values.all { it.isEmpty() }) {
                                // Invoke ShoppingListEmptyContent when the list is empty
                                ShoppingListEmptyContent(
                                    inputText = emptyInputText,
                                    onInputTextChange = { emptyInputText = it },
                                    onAddItem = { name ->
                                        val trimmedName = name.trim()
                                        if (trimmedName.isNotEmpty()) {
                                            onAddIngredients(listOf(trimmedName))
                                            emptyInputText = "" // Clear input after adding
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Item name cannot be empty.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            } else {
                                // Display Shopping List Items and AddShoppingItemSection when list is not empty
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    // Place AddShoppingItemSection under SortingAndFilterRow
                                    AddShoppingItemSection(
                                        inputText = addInputText,
                                        onInputTextChange = { addInputText = it },
                                        onAddItem = {
                                            val trimmedInput = addInputText.trim()
                                            if (trimmedInput.isNotEmpty()) {
                                                onAddIngredients(listOf(trimmedInput))
                                                addInputText = "" // Clear input after adding
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Item name cannot be empty.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    DisplayShoppingList(
                                        categories = categories,
                                        sortAscending = sortAscending,
                                        onIncrease = { item ->
                                            // Update count via repository
                                            val updatedItem = item.copy(count = item.count + 1)
                                            shoppingRepository.updateItem(
                                                item.id!!,
                                                updatedItem
                                            ) { updatedItemResponse ->
                                                if (updatedItemResponse != null) {
                                                    // Update the local list
                                                    val categoryList =
                                                        categories[categorizeItem(item.name)]
                                                    if (categoryList != null) {
                                                        val index = categoryList.indexOf(item)
                                                        if (index != -1) {
                                                            categoryList[index] =
                                                                updatedItemResponse
                                                        }
                                                    }
                                                } else {
                                                    // Handle update failure
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to update item.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        onDecrease = { item ->
                                            if (item.count > 1) {
                                                val updatedItem = item.copy(count = item.count - 1)
                                                shoppingRepository.updateItem(
                                                    item.id!!,
                                                    updatedItem
                                                ) { updatedItemResponse ->
                                                    if (updatedItemResponse != null) {
                                                        // Update the local list
                                                        val categoryList =
                                                            categories[categorizeItem(item.name)]
                                                        if (categoryList != null) {
                                                            val index = categoryList.indexOf(item)
                                                            if (index != -1) {
                                                                categoryList[index] =
                                                                    updatedItemResponse
                                                            }
                                                        }

                                                    } else {
                                                        // Handle update failure
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to update item.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } else {
                                                // Remove the item
                                                categories.values.forEach { list ->
                                                    list.remove(item)
                                                }
                                                // Delete from backend
                                                shoppingRepository.deleteItem(item.id!!) { success ->
                                                    if (!success) {
                                                        // Handle deletion failure
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to delete item.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        },
                                        onDelete = { item ->
                                            // Remove the item locally
                                            categories.values.forEach { list ->
                                                list.remove(item)
                                            }
                                            // Delete from backend
                                            shoppingRepository.deleteItem(item.id!!) { success ->
                                                if (!success) {
                                                    // Handle deletion failure
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to delete item.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        "mealPlanner" -> {
                            MealPlannerContent(
                                navController = navController,
                                recipes = recipes,
                                plannedRecipes = plannedRecipes,
                                notes = notes,
                                mealPlanRepository = mealPlanRepository // Pass the repository
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun SortingAndFilterRow(
    sortAscending: Boolean,
    onSortToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Shopping list",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            /*Icon(
                painter = painterResource(id = android.R.drawable.arrow_down_float),
                contentDescription = "Dropdown Icon",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )*/
        }

        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(onClick = { onSortToggle() }) {
                Icon(
                    painter = painterResource(
                        id = if (sortAscending) android.R.drawable.arrow_up_float else android.R.drawable.arrow_down_float
                    ),
                    contentDescription = "Sort",
                    tint = Color.White
                )
            }
            Text(
                text = "Sorting",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}


// Helper function to categorize items
fun categorizeItem(item: String): String {
    return when (item.lowercase()) {
        // Bakery
        "bread", "croissant", "bagel", "roll", "brioche", "sourdough", "ciabatta", "pita", "focaccia", "muffin", "donut" -> "Bakery"

        // Vegetables
        "potato", "carrot", "onion", "tomato", "cucumber", "broccoli", "cauliflower", "spinach", "lettuce", "pepper", "zucchini", "eggplant", "garlic", "ginger", "cabbage", "sweet potato", "asparagus", "beetroot", "radish" -> "Vegetables"

        // Fruits
        "apple", "banana", "orange", "strawberry", "blueberry", "raspberry", "grape", "watermelon", "pineapple", "kiwi", "mango", "pear", "peach", "cherry", "plum", "apricot", "lemon", "lime", "pomegranate", "fig" -> "Fruits"

        // Dairy
        "milk", "cheese", "butter", "yogurt", "cream", "ice cream", "sour cream", "whipped cream", "cottage cheese", "mozzarella", "parmesan", "feta", "ricotta", "gouda", "brie" -> "Dairy"

        // Meat
        "chicken", "beef", "pork", "lamb", "bacon", "sausage", "ham", "turkey", "duck", "goat", "veal", "ground beef", "salami", "prosciutto" -> "Meat"

        // Seafood
        "salmon", "tuna", "shrimp", "prawns", "crab", "lobster", "oysters", "scallops", "cod", "trout", "mussels", "squid", "clams", "sardines", "anchovies" -> "Seafood"

        // Grains and Pasta
        "rice", "pasta", "spaghetti", "noodles", "quinoa", "couscous", "barley", "oats", "flour", "cornmeal", "breadsticks", "macaroni", "penne" -> "Grains & Pasta"

        // Snacks
        "chips", "popcorn", "pretzels", "crackers", "cookies", "biscuits", "granola bars", "candy", "chocolate", "nuts", "trail mix" -> "Snacks"

        // Drinks
        "water", "juice", "soda", "coffee", "tea", "beer", "wine", "whiskey", "rum", "vodka", "gin", "smoothie", "milkshake" -> "Drinks"

        // Condiments
        "ketchup", "mustard", "mayonnaise", "vinegar", "olive oil", "soy sauce", "hot sauce", "bbq sauce", "salad dressing", "honey", "jam", "peanut butter", "maple syrup" -> "Condiments"

        // Frozen Foods
        "frozen pizza", "frozen vegetables", "ice cream", "frozen fruit", "frozen dinners", "frozen waffles", "frozen fries", "frozen nuggets", "frozen fish" -> "Frozen Foods"

        // Spices and Herbs
        "salt", "pepper", "cinnamon", "nutmeg", "cloves", "paprika", "cumin", "oregano", "thyme", "basil", "parsley", "rosemary", "chili powder", "garlic powder", "ginger powder" -> "Spices & Herbs"

        // Household
        "toilet paper", "paper towels", "laundry detergent", "dish soap", "sponges", "trash bags", "cleaning spray", "aluminum foil", "ziplock bags", "batteries" -> "Household"

        // Personal Care
        "shampoo", "conditioner", "soap", "body wash", "toothpaste", "toothbrush", "deodorant", "lotion", "razors", "shaving cream", "hair gel", "makeup", "face wash", "sunscreen", "moisturizer" -> "Personal Care"

        // Canned Goods
        "canned beans", "canned tomatoes", "canned corn", "canned peas", "canned tuna", "canned salmon", "canned soup", "canned fruit", "canned olives" -> "Canned Goods"

        // Breakfast
        "eggs", "cereal", "oats", "pancake mix", "waffles", "bacon", "sausage", "toast", "jam", "honey", "butter" -> "Breakfast"

        // Baby Products
        "diapers", "baby wipes", "baby food", "baby formula", "baby lotion", "baby shampoo", "baby powder" -> "Baby Products"

        // Pet Supplies
        "dog food", "cat food", "bird seed", "fish food", "litter", "pet shampoo", "pet toys", "pet treats" -> "Pet Supplies"

        else -> "Other"
    }
}


@Composable
fun ShoppingScreenTabs(selectedTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .background(Color(0xFF2E2E2E), shape = MaterialTheme.shapes.medium),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Shopping List Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    if (selectedTab == "shoppingList") Color(0xFF1F1F1F) else Color(0xFF2E2E2E),
                    shape = MaterialTheme.shapes.medium
                )
                .clickable { onTabSelected("shoppingList") }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shopping_cart),
                    contentDescription = "Shopping List Icon",
                    tint = if (selectedTab == "shoppingList") Color(0xFF876232) else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Shopping List",
                    color = if (selectedTab == "shoppingList") Color.White else Color.Gray,
                    fontSize = 16.sp
                )
            }
        }

        // Meal Planner Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    if (selectedTab == "mealPlanner") Color(0xFF1F1F1F) else Color(0xFF2E2E2E),
                    shape = MaterialTheme.shapes.medium
                )
                .clickable { onTabSelected("mealPlanner") }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_meal_planner),
                    contentDescription = "Meal Planner Icon",
                    tint = if (selectedTab == "mealPlanner") Color(0xFF876232) else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Meal Planner",
                    color = if (selectedTab == "mealPlanner") Color.White else Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun TopBars() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp, vertical = 8.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(16f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF876232)
                )
                Spacer(modifier = Modifier.width(1.dp))
                Text(text = "LeGourmand", fontSize = 20.sp, color = Color.White)
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Settings",
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }

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
fun ShoppingListEmptyContent(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onAddItem: (String) -> Unit
) {
    // Main container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.Black),  // If your screen already has a black background, you can remove this
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Icon inside a rounded box with a green outline
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(
                    border = BorderStroke(2.dp, Color(0xFF866232)), // Vibrant green outline
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_shopping_cart),
                contentDescription = "Shopping cart",
                modifier = Modifier.size(40.dp),
                tint = Color(0xFF866232)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Start Your Shopping List",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Begin shopping by add items.",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tip box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 2.dp,
                    color = Color(0xFF2E2E2E), // Change this to your desired border color
                    shape = RoundedCornerShape(12.dp)
                )
                .background(Color(0xFF000000))
                .padding(16.dp)
        ) {
            Text(
                text = "You can also add ingredients from recipes to your shopping list.",
                color = Color(0xFFFEFEFE),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }


        Spacer(modifier = Modifier.height(24.dp))

        // Input field
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            label = {
                Text(
                    text = "I need...",
                    color = Color(0xFFB0BEC5)
                )
            },
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                keyboardType = KeyboardType.Text // Adjusts the type of keyboard
            ),// Ensures a comfortable tap area
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = Color(0xFF876232),
                focusedBorderColor = Color(0xFF876232),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF876232),
                unfocusedLabelColor = Color(0xFFB0BEC5)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button
        Button(
            onClick = { onAddItem(inputText) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF876232)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Start adding",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun OutlinedTextField(value: String, onValueChange: (String) -> Unit, label: () -> Unit, textStyle: TextStyle, modifier: Modifier, colors: TextFieldColors) {

}

@Composable
fun DisplayShoppingList(
    categories: MutableMap<String, SnapshotStateList<ShoppingListItem>>,
    sortAscending: Boolean,
    onIncrease: (ShoppingListItem) -> Unit,
    onDecrease: (ShoppingListItem) -> Unit,
    onDelete: (ShoppingListItem) -> Unit
) {
    val sortedCategories = if (sortAscending) {
        categories.toSortedMap()
    } else {
        categories.toSortedMap(compareByDescending { it })
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        sortedCategories.filter { it.value.isNotEmpty() }.forEach { (category, items) ->
            item {
                Text(
                    text = category,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(items) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color(0xFF1F1F1F), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onDecrease(item) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.remove_24px),
                                contentDescription = "Decrease count",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = item.count.toString(),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { onIncrease(item) }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_input_add),
                                contentDescription = "Increase count",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { onDelete(item) }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_delete),
                                contentDescription = "Delete item",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerContent(
    navController: NavController,
    recipes: List<Recipe>,
    plannedRecipes: MutableMap<LocalDate, SnapshotStateList<Recipe>>,
    notes: SnapshotStateMap<LocalDate, SnapshotStateList<Note>>,
    mealPlanRepository: MealPlanRepository // New parameter
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
// Inside MealPlannerContent.kt

    val today = LocalDate.now() // Get the current date
    var currentStartOfWeek by remember {
        mutableStateOf(
            today.with(
                TemporalAdjusters.previousOrSame(
                    DayOfWeek.SUNDAY
                )
            )
        )
    }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var noteText by remember { mutableStateOf("") }
    var isAddNoteModalVisible by remember { mutableStateOf(false) }

    // Modal Bottom Sheet for adding a note
    if (isAddNoteModalVisible) {
        ModalBottomSheet(
            onDismissRequest = { isAddNoteModalVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Add Note",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Enter note") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done, // Changes keyboard button to "Done"
                        keyboardType = KeyboardType.Text // Adjusts the type of keyboard
                    ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        selectedDay?.let { date ->
                            if (noteText.trim().isNotEmpty()) {
                                val noteDTO = NoteDTO(
                                    id = 0, // Backend assigns the ID
                                    content = noteText.trim()
                                )
                                // Add note via repository
                                coroutineScope.launch {
                                    mealPlanRepository.addNoteToMealPlan(date.toString(), noteDTO) { addedNote ->
                                        if (addedNote != null) {
                                            notes.getOrPut(date) { mutableStateListOf() }.add(
                                                Note(
                                                    id = addedNote.id,
                                                    content = addedNote.content
                                                )
                                            )
                                            noteText = ""
                                            isAddNoteModalVisible = false
                                        } else {
                                            Toast.makeText(context, "Failed to add note.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Note cannot be empty.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF876232)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Note", color = Color.White)
                }
            }
        }
    }

    // Scaffold with no additional BottomBar since it's within ShoppingScreen
    // Implement UI for Meal Planner
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        item {
            // Header for week navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "<",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        currentStartOfWeek = currentStartOfWeek.minusWeeks(1)
                        // Fetch meal plans for the new week
                        val dates = (0..6).map { offset ->
                            currentStartOfWeek.plusDays(offset.toLong())
                        }
                        dates.forEach { date ->
                            mealPlanRepository.getMealPlanForDate(date.toString()) { mealPlan ->
                                if (mealPlan != null) {
                                    plannedRecipes[date] = mutableStateListOf()
                                    mealPlan.recipes.forEach { recipeDTO ->
                                        val recipe = recipes.find { it.id == recipeDTO.id }
                                        recipe?.let { plannedRecipes[date]?.add(it) }
                                    }
                                    notes[date] = mutableStateListOf()
                                    mealPlan.notes.forEach { noteDTO ->
                                        notes[date]?.add(
                                            Note(
                                                id = noteDTO.id,
                                                content = noteDTO.content
                                            )
                                        )
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to load meal plan for ${date.format(DateTimeFormatter.ofPattern("MMM dd"))}.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                )
                Text(
                    text = "Week of ${currentStartOfWeek.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ">",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        currentStartOfWeek = currentStartOfWeek.plusWeeks(1)
                        // Fetch meal plans for the new week
                        val dates = (0..6).map { offset ->
                            currentStartOfWeek.plusDays(offset.toLong())
                        }
                        dates.forEach { date ->
                            mealPlanRepository.getMealPlanForDate(date.toString()) { mealPlan ->
                                if (mealPlan != null) {
                                    plannedRecipes[date] = mutableStateListOf()
                                    mealPlan.recipes.forEach { recipeDTO ->
                                        val recipe = recipes.find { it.id == recipeDTO.id }
                                        recipe?.let { plannedRecipes[date]?.add(it) }
                                    }
                                    notes[date] = mutableStateListOf()
                                    mealPlan.notes.forEach { noteDTO ->
                                        notes[date]?.add(
                                            Note(
                                                id = noteDTO.id,
                                                content = noteDTO.content
                                            )
                                        )
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to load meal plan for ${date.format(DateTimeFormatter.ofPattern("MMM dd"))}.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Generate the days of the current week based on `currentStartOfWeek`
        val daysOfWeek = (0..6).map { offset ->
            val date = currentStartOfWeek.plusDays(offset.toLong())
            Pair(
                date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                date // Full date
            )
        }

        // Days of the week with Plan buttons
        items(daysOfWeek) { (dayName, date) ->
            Column {
                // Day Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1F1F1F), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = dayName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("MMM dd")),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    // Plan Button with Dropdown
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { showMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF866334)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_input_add),
                                contentDescription = "Add Plan",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Plan", color = Color.White)
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add note") },
                                onClick = {
                                    selectedDay = date
                                    isAddNoteModalVisible = true
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Plan random recipe") },
                                onClick = {
                                    // Fetch user's recipes
                                    coroutineScope.launch {
                                        // Retrieve the token correctly using AuthPreferences
                                        val token = AuthPreferences.getToken(context) ?: ""
                                        Log.d("MealPlannerContent", "Retrieved Token: $token")
                                        if (token.isNotEmpty()) {
                                            // Filter out recipes already planned for the selected date
                                            val availableRecipes = recipes.filterNot { recipe ->
                                                plannedRecipes[date]?.any { it.id == recipe.id } == true
                                            }

                                            Log.d("MealPlannerContent", "Available recipes: $availableRecipes")

                                            // Pick a random recipe from the available ones
                                            val randomRecipe = availableRecipes.randomOrNull()

                                            Log.d("MealPlannerContent", "Selected random recipe: $randomRecipe")

                                            if (randomRecipe != null) {
                                                // Add recipe to meal plan via repository
                                                mealPlanRepository.addRecipeToMealPlan(date.toString(), randomRecipe.id!!) { success ->
                                                    if (success) {
                                                        plannedRecipes.getOrPut(date) { mutableStateListOf() }.add(randomRecipe)
                                                        Toast.makeText(context, "Recipe added to meal plan", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to add recipe", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(context, "No available recipes to add", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Authentication token missing.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    showMenu = false
                                }
                            )
                        }
                    }
                }

                // Display Notes for the Day
                notes[date]?.forEachIndexed { index, note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp)
                            .background(Color(0xFF2E2E2E), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = Color(0xFF876232))) {
                                    append("Note ${index + 1}: ")
                                }
                                withStyle(style = SpanStyle(color = Color.White)) {
                                    append(note.content)
                                }
                            },
                            fontSize = 14.sp
                        )

                        var showNoteMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showNoteMenu = true }) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_more),
                                    contentDescription = "Note options",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showNoteMenu,
                                onDismissRequest = { showNoteMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        coroutineScope.launch {
                                            val noteToDelete = notes[date]?.get(index)
                                            if (noteToDelete != null) {
                                                // Call repository to delete the note from the backend
                                                mealPlanRepository.deleteNoteFromMealPlan(
                                                    date.toString(),
                                                    noteToDelete.id
                                                ) { success ->
                                                    if (success) {
                                                        // Remove the note from the local state
                                                        notes[date]?.removeAt(index)
                                                        Toast.makeText(
                                                            context,
                                                            "Note deleted.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to delete note.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Note not found.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        showNoteMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Display Planned Recipes for the Day
                plannedRecipes[date]?.forEachIndexed { index, recipe ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp)
                            .background(Color(0xFF2E2E2E), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.clickable {
                            // Navigate to recipe detail
                            navController.navigate("recipeDetail/${recipe.id}")
                        }) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color(0xFF876232))) {
                                        append("Recipe ${index + 1}: ")
                                    }
                                    withStyle(style = SpanStyle(color = Color.White)) {
                                        append(recipe.title)
                                    }
                                },
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis






                            )
                        }
                        var showRecipeMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showRecipeMenu = true }) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_more),
                                    contentDescription = "Recipe options",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showRecipeMenu,
                                onDismissRequest = { showRecipeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        // Remove recipe from meal plan via repository
                                        coroutineScope.launch {
                                            mealPlanRepository.removeRecipeFromMealPlan(date.toString(), recipe.id!!) { success ->
                                                if (success) {
                                                    plannedRecipes[date]?.remove(recipe)
                                                    Toast.makeText(context, "Recipe removed from meal plan", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to remove recipe", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        showRecipeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
