package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = ForestGreenDark,
  onPrimary = EarthyCanvasDark,
  primaryContainer = SoftBeigeDark,
  onPrimaryContainer = CharcoalBlackDark,
  secondary = SageMutedDark,
  onSecondary = EarthyCanvasDark,
  secondaryContainer = SoftBeigeDark,
  onSecondaryContainer = DarkSlateDark,
  tertiary = EarthyRedDark,
  onTertiary = EarthyCanvasDark,
  tertiaryContainer = SoftBeigeDark,
  onTertiaryContainer = EarthyRedDark,
  background = EarthyCanvasDark,
  onBackground = DarkSlateDark,
  surface = CardDark,
  onSurface = CharcoalBlackDark,
  surfaceVariant = SoftBeigeDark,
  onSurfaceVariant = SageMutedDark,
  outline = SageMutedDark,
  error = EarthyRedDark,
  onError = EarthyCanvasDark
)

private val LightColorScheme = lightColorScheme(
  primary = ForestGreen,
  onPrimary = PureWhite,
  primaryContainer = SoftBeige,
  onPrimaryContainer = CharcoalBlack,
  secondary = SageMuted,
  onSecondary = PureWhite,
  secondaryContainer = SoftBeige,
  onSecondaryContainer = DarkSlate,
  tertiary = EarthyRed,
  onTertiary = PureWhite,
  tertiaryContainer = SoftBeige,
  onTertiaryContainer = EarthyRed,
  background = EarthyCanvas,
  onBackground = DarkSlate,
  surface = PureWhite,
  onSurface = CharcoalBlack,
  surfaceVariant = SoftBeige,
  onSurfaceVariant = SageMuted,
  outline = SageMuted,
  error = EarthyRed,
  onError = PureWhite
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Enforce Natural Tones custom design palette, set default to false
  dynamicColor: Boolean = false,
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
