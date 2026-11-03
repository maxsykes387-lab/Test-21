package com.maxsykes.pixelorganiser.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// Fallback palette (Google blue seed) for phones older than Android 12.
private val LightFallback = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondaryContainer = Color(0xFFC2E7FF),
    onSecondaryContainer = Color(0xFF001D35),
    surface = Color(0xFFF8FAFD),
    surfaceContainerLow = Color(0xFFF0F4F9),
    surfaceContainer = Color(0xFFE9EEF6),
    surfaceContainerHigh = Color(0xFFE1E5EA),
)

private val DarkFallback = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondaryContainer = Color(0xFF004A77),
    onSecondaryContainer = Color(0xFFC2E7FF),
    surface = Color(0xFF131314),
    surfaceContainerLow = Color(0xFF1B1B1C),
    surfaceContainer = Color(0xFF1E1F20),
    surfaceContainerHigh = Color(0xFF282A2C),
)

private val OrganiserTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Medium),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    )
}

private val OrganiserShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Material You: picks up the wallpaper colours on Pixel (Android 12+). */
@Composable
fun PixelOrganiserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkFallback
        else -> LightFallback
    }
    MaterialTheme(
        colorScheme = colors,
        typography = OrganiserTypography,
        shapes = OrganiserShapes,
        content = content,
    )
}
