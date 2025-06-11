package com.example.recipeapp.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recipeapp.R
import com.example.recipeapp.utils.OnBoardingPage
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnBoardingScreen(
    navController: NavController,
    context: Context
) {
    // Retrieve SharedPreferences
    val prefs: SharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

    // Check if onboarding was completed
    val completedOnBoarding = prefs.getBoolean("onboarding_completed", false)
    if (completedOnBoarding) {
        // Immediately navigate to splash and render nothing
        LaunchedEffect(Unit) {
            navController.navigate("splash") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val onBoardingPages = listOf(
        OnBoardingPage(
            title = "Le Gourmand\n\nYour Reliable Cooking Assistant", // ✅ Fix: Add a line break here
            description = "All your recipes, meal planning, and shopping in one place.",
            imageRes = R.drawable.ic_loogo
        ),
        OnBoardingPage(
            title = "Shopping List & Meal Planning",
            description = "Create shopping lists. Prepare your meal plan. Save time and money.",
            imageRes = R.drawable.ic_shop
        ),
        OnBoardingPage(
            title = "One Place for All Your Recipes",
            description = "Save, organize, and access your recipes anywhere.",
            imageRes = R.drawable.ic_recipes
        ),
        OnBoardingPage(
            title = "Your AI Chef Assistant",
            description = "Get personalized recipes based on your preferences and pantry.",
            imageRes = R.drawable.ic_ai
        )
    )


    val pagerState = rememberPagerState()

    Box(modifier = Modifier.fillMaxSize()) {
        // HorizontalPager for swiping through pages
        HorizontalPager(
            count = onBoardingPages.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            OnBoardingPageContent(page = onBoardingPages[pageIndex])
        }

        // Dots indicator above the buttons
        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            activeColor = Color(0xFF2E2C2A),
            inactiveColor = Color.LightGray,
            indicatorWidth = 12.dp,
            indicatorHeight = 12.dp,
            spacing = 8.dp
        )

        // Bottom navigation buttons with updated style
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Skip" button: mark onboarding complete and navigate to splash
            Text(
                text = "Skip",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                modifier = Modifier.clickable {
                    prefs.edit().putBoolean("onboarding_completed", true).apply()
                    navController.navigate("splash") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // "Next" or "Get Started" button
            val isLastPage = pagerState.currentPage == onBoardingPages.size - 1
            Button(
                onClick = {
                    if (isLastPage) {
                        prefs.edit().putBoolean("onboarding_completed", true).apply()
                        navController.navigate("splash") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage + 1,
                                animationSpec = tween(durationMillis = 500)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C6157)),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = if (isLastPage) "Get Started" else "Next",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                )
            }
        }
    }
}

@Composable
fun OnBoardingPageContent(page: OnBoardingPage) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF030303), Color(0xFF886332))
                )
            )
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circular container behind the image
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(110.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = page.imageRes),
                    contentDescription = page.title,
                    modifier = Modifier.size(150.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Split the title by newline character
            val titleParts = page.title.split("\n").filter { it.isNotBlank() }
            if (titleParts.size > 1) {
                // First line: "Le Gourmand" with headline style
                Text(
                    text = titleParts[0],
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Second line: "Your Friendly Cooking Assistant" with a different (smaller) font size
                Text(
                    text = titleParts[1],
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        fontSize = 18.sp // Change this value to adjust font size for the second line
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                // If there's no newline, display the title normally
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFB0BEC5),
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
