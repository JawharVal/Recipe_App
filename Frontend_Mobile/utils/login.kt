import android.util.Log
import android.content.Context
import com.example.recipeapp.network.AppwriteClient
import io.appwrite.models.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun login(context: Context, email: String, password: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            AppwriteClient.initClient(context) // Ensure the client is initialized
            val session: Session = AppwriteClient.account.createSession(email, password)

            Log.d("Login", "Logged in successfully: ${session.userId}")
            true
        } catch (e: Exception) {
            Log.e("Login", "Login failed, Email or Password incorrect.")
            false
        }
    }
}
