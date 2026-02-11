package com.catlover.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// --- Colors ---
private val PrimaryOrange = Color(0xFFFF6F00) // Deep Orange
private val SecondaryTeal = Color(0xFF00BFA5) // Teal Accent
private val DarkBackground = Color(0xFF121212) // True Black/Dark Grey
private val SurfaceDark = Color(0xFF1E1E1E) // Slightly lighter for cards
private val TextWhite = Color(0xFFEEEEEE)
private val TextGray = Color(0xFFAAAAAA)

private val LightPrimary = Color(0xFFFF6F00)
private val LightBackground = Color(0xFFF5F5F5)
private val LightSurface = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    secondary = SecondaryTeal,
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = TextGray
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = SecondaryTeal,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color.Gray
)

// --- Shapes ---
val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun CatLoverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        // Uncomment if you want dynamic system colors (Material You), 
        // but we want a unique brand look, so let's stick to our palette for now.
        // dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        // dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(), // Default Material 3 typography is good enough, we'll style it in components
        shapes = AppShapes,
        content = content
    )
}
