import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recipeapp.R
import com.example.recipeapp.Recipe

// Green Palette
private val SearchBarBackgroundColor = Color(0xFFE8F5E9)    // Very light green
private val FilterButtonBackgroundColor = Color(0xDC328862)   // Medium green
private val SearchTextColor = Color(0xFF000000)               // Dark green
private val SearchPlaceholderColor = Color(0xFF388E3C)        // Vivid green
private val FiltersDropdownBackground = Color(0xD7A5D6A7)     // Light green
private val FilterChipSelectedColor = Color(0xFF66BB6A)       // Deeper green
private val FilterChipUnselectedColor = Color(0xFFC8E6C9)     // Soft green

@Composable
fun SearchBar(
    recipes: List<Recipe>,
    onSearchResult: (List<Recipe>) -> Unit,
    availableTags: List<String>,
    availableCuisines: List<String>,
    availableDifficulties: List<String>
) {
    val searchQuery = remember { mutableStateOf(TextFieldValue("")) }
    val showFilters = remember { mutableStateOf(false) }
    val selectedTags = remember { mutableStateListOf<String>() }
    val selectedCuisine = remember { mutableStateOf<String?>(null) }
    val selectedDifficulty = remember { mutableStateOf<String?>(null) }

    // The parent column animates its size changes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // Top row with search field and filter button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Search Input Field with Icon
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(SearchBarBackgroundColor, shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Search Icon",
                    tint = SearchPlaceholderColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery.value,
                    onValueChange = {
                        searchQuery.value = it
                        filterRecipes(
                            recipes,
                            searchQuery.value.text,
                            selectedTags,
                            selectedCuisine.value,
                            selectedDifficulty.value,
                            onSearchResult
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(color = SearchTextColor),
                    decorationBox = { innerTextField ->
                        if (searchQuery.value.text.isEmpty()) {
                            Text(
                                "Search ...",
                                color = SearchPlaceholderColor,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Filter Button
            Row(
                modifier = Modifier
                    .background(FilterButtonBackgroundColor, shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable { showFilters.value = !showFilters.value },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter),
                    contentDescription = "Filter Icon",
                    tint = SearchTextColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Filters", color = SearchTextColor, fontSize = 14.sp)
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = "Dropdown Icon",
                    tint = SearchTextColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // A spacer to separate the top row from the dropdown
        Spacer(modifier = Modifier.height(8.dp))

        // Animated visibility for filters dropdown (inside a Card)
        AnimatedVisibility(
            visible = showFilters.value,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = FiltersDropdownBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp) // Same as the search bar
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .border(4.dp, Color(0xDF307A59), shape = RoundedCornerShape(10.dp))


            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header title for dropdown
                    Text(

                        style = MaterialTheme.typography.titleMedium,
                        color = SearchTextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tags Section
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.labelSmall,
                        color = SearchTextColor
                    )
                    LazyRow(
                        modifier = Modifier.padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableTags) { tag ->
                            FilterChip(
                                text = tag,
                                isSelected = selectedTags.contains(tag),
                                onSelectedChange = { isSelected ->
                                    if (isSelected) selectedTags.add(tag) else selectedTags.remove(tag)
                                    filterRecipes(
                                        recipes,
                                        searchQuery.value.text,
                                        selectedTags,
                                        selectedCuisine.value,
                                        selectedDifficulty.value,
                                        onSearchResult
                                    )
                                }
                            )
                        }
                    }

                    // Cuisines Section
                    Text(
                        text = "Cuisines",
                        style = MaterialTheme.typography.labelSmall,
                        color = SearchTextColor
                    )
                    LazyRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableCuisines) { cuisine ->
                            FilterChip(
                                text = cuisine,
                                isSelected = selectedCuisine.value == cuisine,
                                onSelectedChange = { isSelected ->
                                    selectedCuisine.value = if (isSelected) cuisine else null
                                    filterRecipes(
                                        recipes,
                                        searchQuery.value.text,
                                        selectedTags,
                                        selectedCuisine.value,
                                        selectedDifficulty.value,
                                        onSearchResult
                                    )
                                }
                            )
                        }
                    }

                    // Difficulties Section
                    Text(
                        text = "Difficulty",
                        style = MaterialTheme.typography.labelSmall,
                        color = SearchTextColor
                    )
                    LazyRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableDifficulties) { difficulty ->
                            FilterChip(
                                text = difficulty,
                                isSelected = selectedDifficulty.value == difficulty,
                                onSelectedChange = { isSelected ->
                                    selectedDifficulty.value = if (isSelected) difficulty else null
                                    filterRecipes(
                                        recipes,
                                        searchQuery.value.text,
                                        selectedTags,
                                        selectedCuisine.value,
                                        selectedDifficulty.value,
                                        onSearchResult
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun Text(style: TextStyle, color: Color) {

}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    // Animate the background color based on selection state
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) FilterChipSelectedColor else FilterChipUnselectedColor,
        animationSpec = tween(durationMillis = 300)
    )
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .background(animatedColor, shape = RoundedCornerShape(20.dp))
            .clickable { onSelectedChange(!isSelected) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text, color = SearchTextColor, fontSize = 14.sp)
    }
}

fun filterRecipes(
    recipes: List<Recipe>,
    query: String,
    tags: List<String>,
    cuisine: String?,
    difficulty: String?,
    onResult: (List<Recipe>) -> Unit
) {
    val filteredRecipes = recipes.filter { recipe ->
        val matchesQuery = query.isEmpty() ||
                recipe.title.contains(query, ignoreCase = true) ||
                recipe.ingredients.contains(query, ignoreCase = true) ||
                recipe.tags.any { it.contains(query, ignoreCase = true) }
        val matchesTags = tags.isEmpty() || recipe.tags.any { it in tags }
        val matchesCuisine = cuisine == null || recipe.cuisine.equals(cuisine, ignoreCase = true)
        val matchesDifficulty = difficulty == null || recipe.difficulty.equals(difficulty, ignoreCase = true)
        matchesQuery && matchesTags && matchesCuisine && matchesDifficulty
    }
    onResult(filteredRecipes)
}
