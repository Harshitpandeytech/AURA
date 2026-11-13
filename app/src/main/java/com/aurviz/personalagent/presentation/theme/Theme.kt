package com.aurviz.personalagent.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ReferenceUI Palette
val DeepSea = Color(0xFF06142B)
val Accent = Color(0xFF7FE3FF)
val TextPrimary = Color(0xFFEAF6FF)
val TextSecondary = Color(0xFFA6C4DA)

private val AuraDarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = DeepSea,
    primaryContainer = Color(0xFF103356),
    onPrimaryContainer = Color(0xFFBCEBFF),
    secondary = Color(0xFF9FE9FF),
    onSecondary = DeepSea,
    secondaryContainer = Color(0xFF132A4A),
    onSecondaryContainer = Color(0xFFD4F4FF),
    tertiary = Color(0xFFB9A8FF),
    onTertiary = Color(0xFF1A134D),
    background = DeepSea,
    onBackground = TextPrimary,
    surface = Color(0xFF091C36),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF132A4A),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF2B4D73),
    outlineVariant = Color(0xFF163252)
)

private val AuraLightColorScheme = lightColorScheme(
    primary = Color(0xFF08688A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBEFFF),
    onPrimaryContainer = Color(0xFF003547),
    secondary = Color(0xFF2E85A3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBEFF9),
    onSecondaryContainer = Color(0xFF003643),
    tertiary = Color(0xFF5A4EB3),
    onTertiary = Color.White,
    background = Color(0xFFF0F8FD),
    onBackground = Color(0xFF071C28),
    surface = Color(0xFFF8FCFF),
    onSurface = Color(0xFF071C28),
    surfaceVariant = Color(0xFFD7EAF3),
    onSurfaceVariant = Color(0xFF456475),
    outline = Color(0xFF6B93A8),
    outlineVariant = Color(0xFFBCD8E6)
)

private val ReplineTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = 1.2.sp
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.8.sp
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.4.sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.2.sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 14.sp,
        letterSpacing = 0.3.sp
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
        fontSize = 12.5.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AuraDarkColorScheme
        else -> AuraLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReplineTypography,
        content = content
    )
}
