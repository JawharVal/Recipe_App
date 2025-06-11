import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.recipeapp.utils.formatRelativeTime
import kotlinx.coroutines.delay
import org.threeten.bp.LocalDateTime

@Composable
fun RelativeTimeText(createdAt: String?) {
    // Update currentTime every minute (for testing you can reduce the delay)
    val currentTime by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000L) // update every minute
        }
    }

    Text(
        text = formatRelativeTime(createdAt, currentTime),
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF878787),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}
