package com.example.recipeapp.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recipeapp.R
import com.example.recipeapp.Recipe
import com.example.recipeapp.components.AddOptionsModal
import com.example.recipeapp.components.AddRecipeBottomSheetContent
import com.example.recipeapp.utils.ApiClient
import com.example.recipeapp.utils.BookRepository
import com.example.recipeapp.utils.RecipeRepository
import com.example.recipeapp.utils.UserDTO
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeSection(navController: NavController, recipes: MutableList<Recipe>) {
    var isModalVisible by remember { mutableStateOf(false) }
    var showAddOptionsModal by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var isSheetVisible by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf("home") }
    // Multi-select mode state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedRecipes = remember { mutableStateListOf<Recipe>() }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Context for token retrieval
    val context = LocalContext.current
    val bookRepository = remember { BookRepository(context) }
    // Fetch recipes when the HomeScreen is first composed
    val recipeRepository = remember { RecipeRepository(context) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_loogo),
            contentDescription = "Add Recipe Icon",
            modifier = Modifier.size(54.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Add first recipe", color = Color.White, fontSize = 18.sp)
        Text("Start your personal recipe collection in seconds now.", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Button to open modal
        OutlinedButton(
            onClick = { showAddOptionsModal = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, Color.Gray),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Add recipe")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Discover recipes", color = Color.White, fontSize = 16.sp)
    }

    // Handle Add Options Modal and Bottom Sheet
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


}
