package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.model.AnimeSeries

private val VicdaruShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun VicdaruTheme(
    activeAnime: AnimeSeries = AnimeSeries.SAKURA,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val targetPrimary = if (darkTheme) activeAnime.darkPrimaryColor else activeAnime.primaryColor
    val targetSecondary = if (darkTheme) activeAnime.darkSecondaryColor else activeAnime.secondaryColor

    val primary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 550),
        label = "themePrimary"
    )
    val secondary by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(durationMillis = 550),
        label = "themeSecondary"
    )
    val bg by animateColorAsState(
        targetValue = if (darkTheme) CyberNightBg else CyberDayBg,
        animationSpec = tween(durationMillis = 400),
        label = "themeBg"
    )
    val surface by animateColorAsState(
        targetValue = if (darkTheme) CyberNightSurface else CyberDaySurface,
        animationSpec = tween(durationMillis = 400),
        label = "themeSurface"
    )
    val surfaceVariant by animateColorAsState(
        targetValue = if (darkTheme) CyberNightSurfaceVariant else CyberDaySurfaceVariant,
        animationSpec = tween(durationMillis = 400),
        label = "themeSurfaceVariant"
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val colorScheme: ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF06131F),
            primaryContainer = primary.copy(alpha = 0.20f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color(0xFF1F0A18),
            secondaryContainer = secondary.copy(alpha = 0.20f),
            onSecondaryContainer = secondary,
            tertiary = VicdaruSkyBlue,
            background = bg,
            onBackground = Color(0xFFF1F5F9),
            surface = surface,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = CyberNightOutline,
            error = Color(0xFFFB7185),
            onError = Color(0xFF2B0610)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.14f),
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.copy(alpha = 0.16f),
            onSecondaryContainer = Color(0xFF1E293B),
            tertiary = VicdaruSkyBlueDeep,
            background = bg,
            onBackground = Color(0xFF0F172A),
            surface = surface,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFF475569),
            outline = CyberDayOutline,
            error = Color(0xFFE11D48),
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = VicdaruShapes,
        content = content
    )
}
