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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AuraDarkColorScheme = darkColorScheme(
    primary = Color(0xFF72D7F2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF174E63),
    onPrimaryContainer = Color(0xFFB8F0FF),
    secondary = Color(0xFFA8E9F5),
    onSecondary = Color(0xFF003640),
    secondaryContainer = Color(0xFF205D6B),
    onSecondaryContainer = Color(0xFFC4F4FF),
    tertiary = Color(0xFFFFB454),
    onTertiary = Color(0xFF452B00),
    background = Color(0xFF071A26),
    onBackground = Color(0xFFE7F8FC),
    surface = Color(0xFF0B2735),
    onSurface = Color(0xFFE7F8FC),
    surfaceVariant = Color(0xFF123D4D),
    onSurfaceVariant = Color(0xFFA7CBD4),
    outline = Color(0xFF3E7180),
    outlineVariant = Color(0xFF214B5A)
)

private val AuraLightColorScheme = lightColorScheme(
    primary = Color(0xFF167A99),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7ECF8),
    onPrimaryContainer = Color(0xFF003544),
    secondary = Color(0xFF3C9BB2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5F2FA),
    onSecondaryContainer = Color(0xFF003640),
    tertiary = Color(0xFFB76A00),
    onTertiary = Color.White,
    background = Color(0xFFE8F8FB),
    onBackground = Color(0xFF102A33),
    surface = Color(0xFFF6FDFF),
    onSurface = Color(0xFF102A33),
    surfaceVariant = Color(0xFFD8F0F5),
    onSurfaceVariant = Color(0xFF496A73),
    outline = Color(0xFF6A9AA4),
    outlineVariant = Color(0xFFB8D7DE)
)

private val MeadowTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 46.sp
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 31.sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
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
        typography = MeadowTypography,
        content = content
    )
}
