package com.example.acurdate.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.acurdate.theme.Typography

enum class PlanetState(val displayName: String) {
    RADIANTE("Radiante 🌞"),
    ESTABLE("Estable 🌿"),
    INERTE("Inerte 🌑"),
    PELIGRO("¡Peligro! 🌋")
}

@Composable
fun PlanetWidget(
    state: PlanetState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "planetTransition")
    
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )
    
    val dangerPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "danger"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .offset(y = floatOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val center = Offset(width / 2, height / 2)
                val radius = width * 0.28f
                
                val (planetColors, ringColor, glowColor) = when (state) {
                    PlanetState.RADIANTE -> Triple(
                        listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFDC2626)),
                        Color(0xFFFBBF24).copy(alpha = 0.7f),
                        Color(0xFFFBBF24).copy(alpha = 0.35f)
                    )
                    PlanetState.ESTABLE -> Triple(
                        listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF047857)),
                        Color(0xFF00F0FF).copy(alpha = 0.7f),
                        Color(0xFF10B981).copy(alpha = 0.35f)
                    )
                    PlanetState.INERTE -> Triple(
                        listOf(Color(0xFF9CA3AF), Color(0xFF4B5563), Color(0xFF1F2937)),
                        Color(0xFF9CA3AF).copy(alpha = 0.2f),
                        Color.Transparent
                    )
                    PlanetState.PELIGRO -> {
                        val pulseAlpha = dangerPulse
                        Triple(
                            listOf(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFF7F1D1D)),
                            Color(0xFFEF4444).copy(alpha = 0.8f * pulseAlpha),
                            Color(0xFFEF4444).copy(alpha = 0.6f * pulseAlpha)
                        )
                    }
                }
                
                // 1. Draw glowing background aura
                if (glowColor != Color.Transparent) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = center,
                            radius = radius * 1.7f
                        ),
                        radius = radius * 1.7f,
                        center = center
                    )
                }
                
                // 2. Draw outer atmospheric ring (halo outline)
                if (state != PlanetState.INERTE) {
                    drawCircle(
                        color = planetColors.first().copy(alpha = 0.3f),
                        radius = radius * 1.12f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                
                // 3. Draw tilted orbital rings with perspective
                rotate(degrees = -18f, pivot = center) {
                    // Rotate the rings over time
                    rotate(degrees = rotationAngle, pivot = center) {
                        scale(scaleX = 1.6f, scaleY = 0.32f, pivot = center) {
                            drawCircle(
                                color = ringColor,
                                radius = radius * 1.25f,
                                center = center,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }
                }
                
                // 4. Draw the main 3D-shaded sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = planetColors,
                        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
                        radius = radius * 1.35f
                    ),
                    radius = radius,
                    center = center
                )
                
                // 5. Draw highlights for a premium glossy glass feel
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(center.x - radius * 0.45f, center.y - radius * 0.45f),
                        radius = radius * 0.55f
                    ),
                    radius = radius,
                    center = center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = state.displayName,
            color = when (state) {
                PlanetState.RADIANTE -> Color(0xFFFBBF24)
                PlanetState.ESTABLE -> Color(0xFF34D399)
                PlanetState.INERTE -> Color(0xFF9CA3AF)
                PlanetState.PELIGRO -> Color(0xFFEF4444)
            },
            style = Typography.bodySmall.copy(fontSize = 11.sp)
        )
    }
}
