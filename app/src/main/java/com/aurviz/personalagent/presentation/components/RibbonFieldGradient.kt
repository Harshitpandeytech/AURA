package com.aurviz.personalagent.presentation.components

import android.graphics.Bitmap
import android.graphics.Color as AColor
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private val Stops = arrayOf(
    0.0432f to Color(0xFFFFFFFF), 0.3318f to Color(0xFFFFFFFF),
    0.3786f to Color(0xFF78B8F9), 0.5814f to Color(0xFF78B8F9),
    0.5886f to Color(0xFF5667FF), 0.7964f to Color(0xFF5667FF),
    0.8000f to Color(0xFF4D2FF9), 1.0000f to Color(0xFF4D2FF9),
)

private const val ANGLE = 32f        // degrees
private const val WAVE = 14f
private const val SPEED = 1.0f       // speed 100 -> 1.00
private const val AMT = 0f           // motionAmount 0
private const val DIR = 1f
private const val GRAIN = 0.21f      // matches CSS opacity 0.210
private const val SLICE = 2f         // px thickness of each cross-axis slice

private fun makeGrain(size: Int = 120, seed: Int = 174074637): ImageBitmap {
    val rnd = Random(seed)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (y in 0 until size) for (x in 0 until size) {
        val v = rnd.nextInt(256)
        bmp.setPixel(x, y, AColor.argb(255, v, v, v))
    }
    return bmp.asImageBitmap()
}

@Composable
fun RibbonFieldGradient(modifier: Modifier = Modifier) {
    val grain = remember { makeGrain() }

    Canvas(modifier) {
        val ph = 0f
        val spin = ph * DIR
        val angle = ANGLE + sin(spin * 0.6f) * 28f * AMT      // 0 at ph = 0
        val clock = 20.75f + ph * 1.2f

        val w = size.width
        val h = size.height
        val a = Math.toRadians(angle.toDouble())
        val len = (abs(w * sin(a)) + abs(h * cos(a))).toFloat() // CSS gradient line length
        val diag = hypot(w, h)

        val brushStops = Stops.map { it.first to it.second }.toTypedArray()

        // Rotate so +x is the CSS gradient direction (angle measured from "up", clockwise)
        rotate(degrees = angle - 90f, pivot = Offset(w / 2f, h / 2f)) {
            val cx = w / 2f
            val cy = h / 2f
            var y = -diag / 2f
            while (y < diag / 2f) {
                val cross = ((y + SLICE / 2f) / diag + 0.5f)
                val off = (WAVE / 100f) * 0.35f *
                    sin(cross * 2.4f * 2f * Math.PI.toFloat() + clock)
                val shift = off * len
                val x0 = cx - len / 2f + shift
                val x1 = cx + len / 2f + shift
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = brushStops,
                        startX = x0,
                        endX = x1,
                        tileMode = TileMode.Clamp
                    ),
                    topLeft = Offset(cx - diag / 2f, cy + y),
                    size = Size(diag, SLICE + 0.5f) // slight overlap avoids seams
                )
                y += SLICE
            }
        }

        // Grain: tiled noise, overlay blend
        drawRect(
            brush = ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated)),
            alpha = GRAIN,
            blendMode = BlendMode.Overlay
        )
    }
}
