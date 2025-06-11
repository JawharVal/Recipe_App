package com.example.recipeapp.utils

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Data classes for ephemeral key requests/responses.
 * You can put these in separate files if you prefer.
 */

/**
 * Data class to hold PaymentIntent response data from the backend.
 */
data class PaymentIntentData(val clientSecret: String, val paymentIntentId: String)

/**
 * StripeManager with Customer + Ephemeral Key flow
 */
class StripeManager(
    private val activity: ComponentActivity,
    private val paymentSheet: PaymentSheet
) {

    //private val BACKEND_PAYMENT_URL = "https://recipeappbackk-production.up.railway.app/api/payment" FOR DEPLOYMENT
    private val BACKEND_BASE_URL = ApiClient.BASE_URL.removeSuffix("/")
    // You can adjust these to point to your actual backend endpoints:
    // private val BACKEND_BASE_URL = "http://192.168.0.137:8081" // Example base URL
    private val CREATE_EPHEMERAL_KEY_URL = "$BACKEND_BASE_URL/api/stripe/create-ephemeral-key"
    private val CREATE_PAYMENT_INTENT_URL = "$BACKEND_BASE_URL/api/payment/create-intent"
    private val CONFIRM_PAYMENT_URL = "$BACKEND_BASE_URL/api/payment/confirm"

    // Plan we’re unlocking (e.g., "PLUS" or "PRO")
    var currentPlanType: String = ""

    // Store the PaymentIntent ID to confirm payment on backend after success
    private lateinit var paymentIntentId: String

    // Callback when payment + backend confirmation are fully successful
    var onPaymentSuccess: (() -> Unit)? = null

    /**
     * Step 1: Start the entire checkout flow.
     * We will:
     *  - Fetch ephemeral key & Stripe customer ID
     *  - Create PaymentIntent for the chosen plan
     *  - Present PaymentSheet with `customer` config so saved cards are available
     */
    fun startCheckout(planType: String) {
        currentPlanType = planType

        Thread {
            try {
                val authToken = AuthPreferences.getToken(activity)
                if (authToken == null) {
                    showToast("Authentication error. Please log in again.")
                    return@Thread
                }

                // 1) Fetch ephemeral key & customer ID
                val ephemeralKeyResponse = fetchEphemeralKey(authToken)
                    ?: throw Exception("Failed to fetch ephemeral key")

                // 2) Create a PaymentIntent for this plan
                val paymentIntentData = createPaymentIntent(planType, authToken)
                    ?: throw Exception("Failed to create payment intent")

                paymentIntentId = paymentIntentData.paymentIntentId

                // 3) Present the PaymentSheet with CustomerConfiguration so user can see saved cards
                activity.runOnUiThread {
                    val configuration = PaymentSheet.Configuration(
                        merchantDisplayName = "LeGourmand",
                        customer = PaymentSheet.CustomerConfiguration(
                            id = ephemeralKeyResponse.customerId,
                            ephemeralKeySecret = ephemeralKeyResponse.ephemeralKey
                        )
                    )
                    paymentSheet.presentWithPaymentIntent(paymentIntentData.clientSecret, configuration)
                }

            } catch (e: Exception) {
                Log.e("StripeManager", "startCheckout Error: ${e.message}")
                showToast("Payment failed: ${e.message}")
            }
        }.start()
    }

    /**
     * Step 2: Handle PaymentSheet results.
     * If successful, we confirm on backend to finalize subscription.
     */
    fun handlePaymentResult(paymentResult: PaymentSheetResult) {
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
                // PaymentSheet says payment succeeded on Stripe side
                showToast("Payment successful!")
                // Confirm on your backend to update subscription, etc.
                confirmPaymentOnBackend(currentPlanType)
            }
            is PaymentSheetResult.Canceled -> {
                showToast("Payment canceled")
            }
            is PaymentSheetResult.Failed -> {
                showToast("Payment failed: ${paymentResult.error.message}")
            }
        }
    }

    /**
     * Step 3: Confirm the payment on your backend (updates subscription).
     */
    private fun confirmPaymentOnBackend(planType: String) {
        Thread {
            try {
                val authToken = AuthPreferences.getToken(activity)
                if (authToken == null) {
                    Log.e("StripeManager", "Error: No auth token found")
                    return@Thread
                }

                val url = URL(CONFIRM_PAYMENT_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $authToken")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("paymentIntentId", paymentIntentId)
                    put("planType", planType)
                }
                conn.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray())
                    os.flush()
                }

                val responseCode = conn.responseCode
                val responseStream =
                    if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val responseText = responseStream?.bufferedReader()?.use { it.readText() } ?: ""

                Log.d(
                    "StripeManager",
                    "confirmPaymentOnBackend: code=$responseCode body=$responseText"
                )

                if (responseCode == 200) {
                    showToast("Subscription updated successfully!")
                    onPaymentSuccess?.invoke()
                } else {
                    showToast("Failed to update subscription: $responseText")
                }

            } catch (e: Exception) {
                Log.e("StripeManager", "Error confirming payment: ${e.localizedMessage}")
                showToast("Error confirming payment: ${e.message}")
            }
        }.start()
    }

    /**
     * Fetch ephemeral key + Stripe customerId from your backend
     */
    private fun fetchEphemeralKey(authToken: String): EphemeralKeyResponse? {
        val url = URL(CREATE_EPHEMERAL_KEY_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $authToken")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        // We must pass the Stripe API version for ephemeral keys
        val bodyJson = JSONObject().apply {
            put("apiVersion", "2022-11-15") // Or your chosen Stripe API version
        }

        conn.outputStream.use { os ->
            os.write(bodyJson.toString().toByteArray())
            os.flush()
        }

        val responseCode = conn.responseCode
        val responseStream =
            if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val responseText = responseStream?.bufferedReader()?.use { it.readText() } ?: ""

        return if (responseCode == 200) {
            Log.d("StripeManager", "EphemeralKey response: $responseText")
            val obj = JSONObject(responseText)
            EphemeralKeyResponse(
                ephemeralKey = obj.getString("ephemeralKey"),
                customerId = obj.getString("customerId")
            )
        } else {
            Log.e("StripeManager", "Failed to fetch ephemeral key: $responseCode $responseText")
            null
        }
    }

    /**
     * Create a PaymentIntent for the given plan type (PLUS, PRO, etc.)
     */
    private fun createPaymentIntent(planType: String, authToken: String): PaymentIntentData? {
        val urlString = "$CREATE_PAYMENT_INTENT_URL?plan=$planType"
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $authToken")
        conn.doOutput = true

        val responseCode = conn.responseCode
        val responseStream =
            if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val responseText = responseStream?.bufferedReader()?.use { it.readText() } ?: ""

        return if (responseCode == 200) {
            val jsonObject = JSONObject(responseText)
            val clientSecret = jsonObject.getString("clientSecret")
            val paymentIntentId = jsonObject.getString("paymentIntentId")
            PaymentIntentData(clientSecret, paymentIntentId)
        } else {
            Log.e("StripeManager", "createPaymentIntent error: $responseCode $responseText")
            null
        }
    }

    /**
     * Utility to show Toast on UI thread
     */
    private fun showToast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }
}
