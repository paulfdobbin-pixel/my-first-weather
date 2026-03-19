package com.example.myfirstweather.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun DynamicWeatherEffect(code: Int, isDay: Boolean) {
    when (code) {
        0 -> if (isDay) SunGlowEffect() else CloudEffect() 
        1, 2, 3 -> CloudEffect()
        61, 63, 65, 80, 81, 82 -> RainEffect()
        95, 96, 99 -> RainEffect()
    }
}

@Composable
fun RainEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val rainDropCount = 80
    val drops = remember {
        List(rainDropCount) {
            RainDrop(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 2f - 1f,
                speed = 0.04f + Random.nextFloat() * 0.08f,
                length = 15f + Random.nextFloat() * 25f
            )
        }
    }

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drops.forEach { drop ->
            val currentY = (drop.y + progress * drop.speed * 20f) % 1f
            val start = Offset(drop.x * size.width, currentY * size.height)
            val end = Offset(drop.x * size.width, (currentY * size.height) + drop.length)
            
            drawLine(
                color = Color(0xFF81D4FA).copy(alpha = 0.25f),
                start = start,
                end = end,
                strokeWidth = 1.5f
            )
        }
    }
}

@Composable
fun SunGlowEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "sunglow")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFE0B2).copy(alpha = 0.2f),
                    Color(0xFFFFCCBC).copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.15f),
                radius = size.width * 0.9f * scale
            )
        )
    }
}

@Composable
fun CloudEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color.White.copy(alpha = 0.1f),
            radius = size.width * 0.45f,
            center = Offset(offsetX * size.width, size.height * 0.12f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = size.width * 0.35f,
            center = Offset((offsetX - 0.4f) * size.width, size.height * 0.18f)
        )
    }
}

data class RainDrop(val x: Float, val y: Float, val speed: Float, val length: Float)
