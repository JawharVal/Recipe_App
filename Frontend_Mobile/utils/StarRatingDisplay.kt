package com.example.recipeapp.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StarRatingDisplay(rating: Float) {
    Row {
        val fullStars = rating.toInt()
        val hasHalfStar = (rating - fullStars) >= 0.5f
        for (i in 1..5) {
            if (i <= fullStars) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Full Star",
                    tint = Color(0xFFFFC107), // Amber color
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = "Empty Star",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
