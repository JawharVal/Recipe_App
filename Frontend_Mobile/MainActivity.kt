package com.example.recipeapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import com.example.recipeapp.ui.theme.RecipeAppTheme
import com.example.recipeapp.navigation.AppNavigation
import com.example.recipeapp.network.AppwriteClient
import com.example.recipeapp.utils.StripeManager
import com.jakewharton.threetenabp.AndroidThreeTen
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet

class MainActivity : ComponentActivity() {
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var stripeManager: StripeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Stripe SDK
        PaymentConfiguration.init(this, "pk_test_51QxALvFWpvGDokVtGO1gss3bUkhHeZjlvwN6Yv3PW1NzCmzOufTQqdlaEauzL6Ma0TiQorbOfZDWskDoynmzuKNE00JSVbxo2B")

        // Initialize PaymentSheet BEFORE RESUMED state
        paymentSheet = PaymentSheet(this) { paymentResult ->
            stripeManager.handlePaymentResult(paymentResult)  // Call handlePaymentResult instead of non-existing function
        }

        // Pass PaymentSheet instance to StripeManager
        stripeManager = StripeManager(this, paymentSheet)

        AppwriteClient.initClient(this)
        val recipes = mutableStateListOf<Recipe>()
        AndroidThreeTen.init(this)

        setContent {
            RecipeAppTheme {
                AppNavigation(recipes, stripeManager)
            }
        }
    }
}
