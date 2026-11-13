package com.aurviz.personalagent.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aurviz.personalagent.R

enum class AuraAgentState {
    IDLE,
    LISTENING,
    THINKING
}
@Composable
fun MrAuraAvatar(
    state: AuraAgentState,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    audioRms: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_effects")

    // Revolving ring rotation for THINKING state
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "revolving_halo"
    )

    // Reverse ring rotation
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_revolving_halo"
    )

    // Breathing pulse for IDLE state
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_pulse"
    )

    // Audio reactive pulse when listening
    val normalizedRms = (audioRms.coerceIn(0f, 12f) / 12f)
    val dynamicListeningScale = 1f + (normalizedRms * 0.15f)

    val currentScale = when (state) {
        AuraAgentState.IDLE -> breathingScale
        AuraAgentState.LISTENING -> dynamicListeningScale
        AuraAgentState.THINKING -> 1.02f
    }

    val imageRes = when (state) {
        AuraAgentState.IDLE -> R.drawable.mr_aura_alltime
        AuraAgentState.LISTENING -> R.drawable.mr_aura_listening
        AuraAgentState.THINKING -> R.drawable.mr_aura_understood
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(currentScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer animated glowing / revolving halos
        when (state) {
            AuraAgentState.THINKING -> {
                // Outer orbital gradient ring (Image 1 reference)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAngle)
                        .border(
                            width = (size.value * 0.035f).coerceAtLeast(2f).dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF6B4A),
                                    Color(0xFF00E5FF),
                                    Color(0xFFB388FF),
                                    Color(0xFFFF6B4A)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Secondary inner orbital ring spinning in reverse
                Box(
                    modifier = Modifier
                        .size(size * 0.88f)
                        .rotate(reverseRotation)
                        .border(
                            width = (size.value * 0.02f).coerceAtLeast(1.5f).dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0x00FFFFFF),
                                    Color(0xFF00E5FF),
                                    Color(0x00FFFFFF),
                                    Color(0xFFFF6B4A)
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            AuraAgentState.LISTENING -> {
                // Expanding soundwave aura
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.5.dp,
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0xFFFF6B4A).copy(alpha = 0.8f),
                                    Color(0xFF00E5FF).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            AuraAgentState.IDLE -> {
                // Subtle static ambient neon border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0x40FFFFFF),
                                    Color(0x10FFFFFF)
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
        }

        // Avatar Core
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Mr. Aura",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size * 0.82f)
                .clip(CircleShape)
                .background(Color.Black)
        )
    }
}
