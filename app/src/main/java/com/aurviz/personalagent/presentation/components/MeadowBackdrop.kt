package com.aurviz.personalagent.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.sin

@Composable
fun MeadowBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "meadow_motion")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hill_drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF121A26), Color(0xFF27324A), Color(0xFF0D1510))
            )
        )

        val starPoints = listOf(
            0.06f to 0.12f, 0.18f to 0.08f, 0.31f to 0.16f, 0.47f to 0.07f,
            0.62f to 0.13f, 0.78f to 0.06f, 0.9f to 0.18f, 0.14f to 0.28f,
            0.72f to 0.25f, 0.84f to 0.32f
        )
        starPoints.forEachIndexed { index, (x, y) ->
            val alpha = 0.22f + 0.16f * sin((drift * 6.28f) + index).toFloat()
            drawCircle(
                color = Color(0xFFE7EBE1).copy(alpha = alpha),
                radius = if (index % 3 == 0) 1.8f else 1f,
                center = Offset(size.width * x, size.height * y)
            )
        }

        drawCircle(
            color = Color(0xFFC9D8FF).copy(alpha = 0.9f),
            radius = size.minDimension * 0.035f,
            center = Offset(size.width * 0.82f, size.height * 0.1f)
        )

        drawHill(
            baseY = size.height * 0.72f,
            amplitude = size.height * 0.09f,
            color = Color(0xFF3B4859),
            offset = drift * size.width * 0.02f
        )
        drawHill(
            baseY = size.height * 0.79f,
            amplitude = size.height * 0.08f,
            color = Color(0xFF253432),
            offset = -drift * size.width * 0.035f
        )
        drawHill(
            baseY = size.height * 0.88f,
            amplitude = size.height * 0.07f,
            color = Color(0xFF19281E),
            offset = drift * size.width * 0.05f
        )
        drawRect(
            color = Color(0xFF0D1510),
            topLeft = Offset(0f, size.height * 0.88f),
            size = size.copy(height = size.height * 0.12f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHill(
    baseY: Float,
    amplitude: Float,
    color: Color,
    offset: Float
) {
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, baseY)
        val points = 12
        for (index in 0..points) {
            val x = size.width * index / points
            val wave = sin((index * 0.9f) + offset / size.width * 6.28f) * amplitude
            lineTo(x, baseY + wave)
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}
