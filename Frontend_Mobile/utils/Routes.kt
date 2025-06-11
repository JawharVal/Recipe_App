package com.example.recipeapp.utils


sealed class Routes(val route: String) {
    object Splash : Routes("splash")
    object Login : Routes("login")
    object Register : Routes("register")
    object Home : Routes("home")
    object Profile : Routes("profile")
    object Shopping : Routes("shopping")
    object Cookbooks : Routes("cookbooks")
    object CookbookDetail : Routes("cookbookDetail/{bookId}") {
        fun createRoute(bookId: Long) = "cookbookDetail/$bookId"
    }
    // Add other routes as needed
}
