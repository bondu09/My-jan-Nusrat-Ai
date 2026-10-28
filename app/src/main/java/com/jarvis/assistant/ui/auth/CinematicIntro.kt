package com.jarvis.assistant.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun CinematicIntro(onFinished: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.5f) }
    val textAlphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Initial logo pop
        alphaAnim.animateTo(1f, tween(1000))
        scaleAnim.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        
        delay(500)
        // Reveal text
        textAlphaAnim.animateTo(1f, tween(800))
        
        delay(2000)
        // Finish intro
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Background Glow Effect
        GlowBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Logo (Simulated with a Circle and Glow for now)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scaleAnim.value * pulseScale)
                    .alpha(alphaAnim.value),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF7A00), Color(0x00FF7A00)),
                            center = center,
                            radius = radius * 1.5f
                        ),
                        radius = radius * 1.5f,
                        alpha = 0.3f
                    )
                    drawCircle(
                        color = Color(0xFFFF7A00),
                        radius = radius,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFF7A00),
                        radius = radius * 0.4f
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "NUSRAT AI",
                color = Color(0xFFFF7A00),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(textAlphaAnim.value)
            )

            Text(
                text = "INTELLIGENCE REDEFINED",
                color = Color(0xFF777777),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(textAlphaAnim.value)
            )
        }
    }
}

@Composable
fun GlowBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        for (i in 0..3) {
            val offset = i * (Math.PI / 2).toFloat()
            val x = width / 2 + (width / 4) * sin(phase + offset)
            val y = height / 2 + (height / 4) * sin(phase * 0.7f + offset)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x11FF7A00), Color.Transparent),
                    center = Offset(x, y),
                    radius = 400.dp.toPx()
                ),
                radius = 400.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
