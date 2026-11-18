package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = StudioGoldDark,
    onPrimary = Color(0xFF281800),
    primaryContainer = Color(0xFF533400),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFD5C4B4),
    onSecondary = Color(0xFF3A2E24),
    tertiary = Color(0xFFE5BE65),
    background = StudioObsidianDark,
    onBackground = Color(0xFFF7F5F0),
    surface = StudioSurfaceDark,
    onSurface = Color(0xFFF7F5F0),
    surfaceVariant = StudioSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCCC5B9),
    outline = StudioBorderDark,
    outlineVariant = Color(0xFF383C4D)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = StudioGoldLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B8),
    onPrimaryContainer = Color(0xFF382200),
    secondary = Color(0xFF6C5D4F),
    onSecondary = Color.White,
    tertiary = Color(0xFF7F5B00),
    background = StudioLinenLight,
    onBackground = StudioInkLight,
    surface = StudioSurfaceLight,
    onSurface = StudioInkLight,
    surfaceVariant = StudioSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF5A5F6E),
    outline = StudioBorderLight,
    outlineVariant = Color(0xFFD6DAE4)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
