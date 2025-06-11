package com.example.recipeapp.network

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Storage

object AppwriteClient {
    lateinit var client: Client
        private set  // Make it accessible but not modifiable

    lateinit var storage: Storage
        private set

    lateinit var account: Account
        private set

    fun initClient(context: Context) {
        if (!::client.isInitialized) {
            client = Client(context)
                .setEndpoint("https://cloud.appwrite.io/v1") // Appwrite Cloud Endpoint
                .setProject("67b7ea4e000d06d8d51a") // Your Project ID
                .setSelfSigned(true) // Allow self-signed certificate (for local testing)

            storage = Storage(client)
            account = Account(client)
        }
    }
}
