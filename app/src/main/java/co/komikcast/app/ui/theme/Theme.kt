package co.komikcast.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val StaticDarkScheme = darkColorScheme(
    primary = Color(0xFFBBA7FF),
    onPrimary = Color(0xFF1E1148),
    secondary = Color(0xFF7DFFDD),
    onSecondary = Color(0xFF00201A),
    tertiary = Color(0xFFFFB3D5),
    background = Color(0xFF05050B),
    onBackground = Color(0xFFE9E5F5),
    surface = Color(0xFF11111D),
    onSurface = Color(0xFFE9E5F5),
    surfaceVariant = Color(0xFF242437),
    onSurfaceVariant = Color(0xFFC9C2D8),
    error = Color(0xFFFFB4AB)
)

private val KomikcastShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

@Composable
fun KomikcastTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context) else StaticDarkScheme
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        shapes = KomikcastShapes,
        content = content
    )
}
