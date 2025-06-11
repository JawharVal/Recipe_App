package com.example.recipeapp.utils


import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.recipeapp.R
import com.example.recipeapp.Recipe

import com.example.recipeapp.utils.RecipeRepository
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable

import androidx.compose.material.icons.filled.Report
import androidx.compose.ui.unit.dp

@Composable
fun ReviewItem(review: ReviewDTO, recipe: Recipe) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }
    // Predefined list of report reasons
    var authorDetails by remember { mutableStateOf<UserDTO?>(null) }

    val reportReasons = listOf(
        "Inappropriate name/language",
        "Spam",
        "Harassment",
        "Other"
    )
    val formattedDate = try {
        review.createdAt?.let {
            LocalDateTime.parse(it).format(formatter)
        } ?: "Unknown Date"
    } catch (e: Exception) {
        "Unknown Date"
    }
    LaunchedEffect(review.userId) {  // <-- Fetch review author's details, NOT recipe author!
        Log.d("ReviewDetail", "Attempting to fetch review author details for ID: ${review.userId}")
        review.userId?.let { id ->
            ApiClient.getAuthService(context).getUserById(id).enqueue(object : Callback<UserDTO> {
                override fun onResponse(call: Call<UserDTO>, response: Response<UserDTO>) {
                    if (response.isSuccessful) {
                        authorDetails = response.body()
                        Log.d("RecipeDetail", "Fetched author details: $authorDetails")
                    } else {
                        Log.e("RecipeDetail", "Error fetching author details: ${response.code()} ${response.message()}")
                    }
                }
                override fun onFailure(call: Call<UserDTO>, t: Throwable) {
                    Log.e("RecipeDetail", "Error fetching author details: ${t.localizedMessage}")
                }
            })
        }
    }
    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(Color.DarkGray, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // User Avatar or Icon
            if (authorDetails != null) {
                if (!authorDetails!!.imageUri.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = authorDetails!!.imageUri),
                        contentDescription = "Author Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                    )
                } else {
                    // Fallback: display a circle with the author's initials
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = authorDetails!!.username?.take(2)?.uppercase() ?: "NN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            } else {
                // While author details are loading, show a placeholder image
                Image(
                    painter = painterResource(id = R.drawable.ic_discover),
                    contentDescription = "Placeholder Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = review.username ?: "Unknown User",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = formattedDate,
                    fontWeight = FontWeight.Light,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            // Report icon
            IconButton(onClick = { showReportDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Report review",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        // Show the ReportReasonDialog if requested
        if (showReportDialog) {
            ReportReasonDialog(
                reasons = reportReasons,
                onDismiss = { showReportDialog = false },
                onReasonSelected = { selectedReason ->
                    // When a reason is selected, trigger the report API call
                    reportReview(review.id ?: 0L, selectedReason, context)
                    showReportDialog = false
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        StarRatingDisplay(rating = review.rating.toFloat())
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = censorBadWords(review.comment),
            color = Color.LightGray,
            fontSize = 14.sp
        )
    }
}
fun reportReview(reviewId: Long, reason: String, context: Context) {
    val authService = ApiClient.getAuthService(context)
    authService.reportReview(reviewId, reason)
        .enqueue(object : Callback<ReviewReportDTO> {
            override fun onResponse(
                call: Call<ReviewReportDTO>,
                response: Response<ReviewReportDTO>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Review reported.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "You have already reported this review.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ReviewReportDTO>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
}
@Composable
fun ReportReasonDialog(
    reasons: List<String>,
    onDismiss: () -> Unit,
    onReasonSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Report Review",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White // Ensure readability
            )
        },
        text = {
            Column {
                Text(
                    text = "Select a reason:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.LightGray, // Subtle contrast for secondary text
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Divider(color = Color.Gray)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 500.dp)
                ) {
                    items(reasons) { reason ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                                .clickable { onReasonSelected(reason) },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF424242) // Darker gray for item background
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.elevatedCardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Report,
                                    contentDescription = "Report Icon",
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White // Text remains visible on dark background
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = Color.White, // Ensures the button text is visible
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFF252322) // Set dark background color for dialog
    )
}
fun censorBadWords(input: String): String {
    // List of regex patterns to capture various forms of banned words
    val bannedPatterns = listOf(
        // English examples
        Regex("(?i)\\bfuck(?:ed|ing|er)?\\b"),
        Regex("(?i)\\bshit(?:ty|ed)?\\b"),
        Regex("(?i)\\bbitch\\b"),
        Regex("(?i)\\basshole\\b"),
        Regex("(?i)\\bdick\\b"),
        Regex("(?i)\\bcunt(?:s|ing)?\\b"),
        Regex("(?i)\\bmotherfucker(?:ed|ing|s)?\\b"),
        Regex("(?i)\\bbastard(?:s)?\\b"),
        Regex("(?i)\\bdamn(?:ed)?\\b"),
        Regex("(?i)\\bcrap\\b"),
        Regex("(?i)\\bpiss(?:ed|ing)?\\b"),
        Regex("(?i)\\bslut(?:ty)?\\b"),
        Regex("(?i)\\bdouche(?:bag)?\\b"),
        Regex("(?i)\\bfagg?ot\\b"),
        Regex("(?i)\\bwhore\\b"),
        Regex("(?i)\\bbollocks\\b"),
        Regex("(?i)\\barsehole\\b"),
        Regex("(?i)\\btwat\\b"),
        Regex("(?i)\\bbugger\\b"),
        Regex("(?i)\\bshag(?:ging)?\\b"),
        Regex("(?i)\\bwanker\\b"),

        // Russian examples
        Regex("(?i)\\bбляд(?:ь|ный|и)?\\b"),
        Regex("(?i)\\bсука\\b"),
        Regex("(?i)\\bхуй(?:[а-я]*)?\\b"),
        Regex("(?i)\\bпизда\\b"),
        Regex("(?i)\\bеб(?:ать|ается|ался|ут|ют|ешь|ала)?\\b"),
        Regex("(?i)\\bёб(?:ать|ается|ался|ут|ют|ешь|ала)?\\b"),
        Regex("(?i)\\bнахуй\\b"),
        Regex("(?i)\\bмудило\\b"),
        Regex("(?i)\\bговно\\b"),
        Regex("(?i)\\bдерьмо\\b"),
        Regex("(?i)\\bпиздец\\b"),
        Regex("(?i)\\bзалупа\\b"),
        Regex("(?i)\\bпидор(?:а|ы|ов)?\\b"),
        Regex("(?i)\\bхер(?:[а-я]*)?\\b"),
        Regex("(?i)\\bтрах(?:аться|аюсь|ался|ались)?\\b"),
        Regex("(?i)\\bсучка\\b")
    )

    var output = input
    for (pattern in bannedPatterns) {
        output = pattern.replace(output) { matchResult ->
            "*".repeat(matchResult.value.length)
        }
    }
    return output
}
