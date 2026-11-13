package com.aurviz.personalagent.presentation.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val ORB_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;

    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453123);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    }

    float fbm(float2 p) {
        float v = 0.0;
        float a = 0.5;
        for (int i = 0; i < 4; i++) {
            v += a * noise(p);
            p *= 2.0;
            a *= 0.5;
        }
        return v;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution;
        float2 p = uv * 2.5;
        float q = fbm(p + float2(iTime * 0.25, iTime * 0.15));
        float r = fbm(p + 3.0 * q + float2(iTime * 0.35, -iTime * 0.2));
        float f = fbm(p + 2.5 * r);

        float3 deepBlue = float3(0.32, 0.55, 0.98);
        float3 softBlue = float3(0.76, 0.85, 1.00);
        float3 cloudWhite = float3(0.96, 0.98, 1.00);

        float3 color = mix(
            softBlue,
            deepBlue,
            smoothstep(0.35, 0.85, 1.0 - uv.y + (f - 0.5) * 0.45)
        );
        color = mix(color, cloudWhite, smoothstep(0.35, 0.75, f));
        return half4(color, 1.0);
    }
"""

@Composable
fun AiOrbiter(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        OrbiterShader(modifier, size)
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {}
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun OrbiterShader(
    modifier: Modifier,
    size: Dp
) {
    val shader = remember { RuntimeShader(ORB_SHADER) }
    val brush = remember(shader) { ShaderBrush(shader) }
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_time"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            shader.setFloatUniform("iResolution", this.size.width, this.size.height)
            shader.setFloatUniform("iTime", time)
            drawCircle(
                brush = brush,
                radius = this.size.minDimension / 2f
            )
        }
    }
}
