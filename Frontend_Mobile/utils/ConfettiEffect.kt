package com.example.recipeapp.utils

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.*

@Composable
fun ConfettiEffect(show: Boolean) {
    if (show) {
        val composition by rememberLottieComposition(LottieCompositionSpec.Asset("confetti.json"))
        // Play the animation once (iterations = 1) as an overlay
        LottieAnimation(
            composition = composition,
            iterations = 1,
            modifier = Modifier.fillMaxSize()
        )
    }
}
