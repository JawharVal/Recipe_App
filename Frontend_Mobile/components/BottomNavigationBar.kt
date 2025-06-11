/*package com.example.recipeapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeapp.R

@Composable
fun BottomNavigationBar(
    navController: NavController,
    selectedTab: String, // Pass the currently selected tab as a parameter
    onTabSelected: (String) -> Unit, // Callback to handle tab selection
    onAddClick: () -> Unit
) {
    Column {
        // Separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Gray.copy(alpha = 0.3f)) // Light gray line
        )

        // Bottom navigation bar
        NavigationBar(
            containerColor = Color.Black,  // Background color of the navigation bar
            contentColor = Color.White     // Default color for items
        ) {
            NavigationBarItem(
                icon = { Icon(painter = painterResource(id = R.drawable.ic_recipes), contentDescription = "Recipes") },
                label = { Text("Recipes") },
                selected = selectedTab == "home",
                onClick = {
                    onTabSelected("home")
                    navController.navigate("home") // Use "home" instead of "recipes"
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray
                )
            )

            NavigationBarItem(
                icon = { Icon(painter = painterResource(id = R.drawable.ic_shopping), contentDescription = "Shopping") },
                label = { Text("Shopping") },
                selected = selectedTab == "shopping", // Dynamically check if this is the selected tab
                onClick = {
                    onTabSelected("shopping")
                    navController.navigate("shopping")
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray
                )
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_recipe),
                        contentDescription = "Add",
                        modifier = Modifier.size(55.dp) // Adjust the size as needed
                    )
                },
                label = { Text("") },
                selected = false,
                onClick = { onAddClick() }, // Trigger the modal bottom sheet
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White
                )
            )

            NavigationBarItem(
                icon = { Icon(painter = painterResource(id = R.drawable.ic_cookbooks), contentDescription = "Cookbooks") },
                label = { Text("Books") },
                selected = selectedTab == "cookbooks", // Dynamically check if this is the selected tab
                onClick = {
                    onTabSelected("cookbooks")
                    navController.navigate("cookbooks")
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray
                )
            )

            NavigationBarItem(
                icon = { Icon(painter = painterResource(id = R.drawable.ic_profile), contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = selectedTab == "profile", // Dynamically check if this is the selected tab
                onClick = {
                    onTabSelected("profile")
                    navController.navigate("profile")
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

*/

package com.example.recipeapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recipeapp.R

@Composable
fun BottomNavigationBar(
    navController: NavController,
    selectedTab: String, // Pass the currently selected tab as a parameter
    onTabSelected: (String) -> Unit,
    // Callback to handle tab selection
    onAddClick: () -> Unit
) {
    Column {
        // Separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Gray.copy(alpha = 0.3f)) // Light gray line
        )

        // Bottom navigation bar
        NavigationBar(
            containerColor = Color(0xFF1F1F1F),  // Background color of the navigation bar
            contentColor = Color.White     // Default color for items
        ) {

            NavigationBarItem(
                icon = { Icon(painter = painterResource(id = R.drawable.ic_recipes), contentDescription = "Recipes") },
                label = { Text("Recipes") },
                selected = selectedTab == "home",
                onClick = {
                    onTabSelected("home")
                    navController.navigate("home")
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF328762), // The background behind the selected item
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )


            NavigationBarItem(
                icon = { Icon(painter = painterResource(id = R.drawable.ic_shopping), contentDescription = "Shopping") },
                label = { Text("Shopping") },
                selected = selectedTab == "shopping", // Dynamically check if this is the selected tab
                onClick = {
                    onTabSelected("shopping")
                    navController.navigate("shopping")
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF328762), // The background behind the selected item
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_recipe),
                        contentDescription = "Add",
                        modifier = Modifier.size(55.dp) // Adjust the size as needed
                    )
                },
                label = { Text("") },
                selected = false,
                onClick = { onAddClick() }, // Trigger the modal bottom sheet
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF328762), // The background behind the selected item
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cookbooks),
                        contentDescription = "Cookbooks"
                    )
                },
                label = { Text("Books") },
                selected = selectedTab == "cookbooks",
                onClick = {
                    onTabSelected("cookbooks")
                    navController.navigate("cookbooks") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF328762), // The background behind the selected item
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )

            NavigationBarItem(
                icon = { Icon(painter = painterResource(id = R.drawable.ic_profile), contentDescription = "Profile") },
                label = { Text("Profile") },
                selected = selectedTab == "profile", // Dynamically check if this is the selected tab
                onClick = {
                    onTabSelected("profile")
                    navController.navigate("profile")
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF328762), // The background behind the selected item
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

