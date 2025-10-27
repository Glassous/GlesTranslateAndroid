package com.glassous.glestranslate.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A modern, shape-shifting loading indicator inspired by MD3 Expressive (2025),
 * morphing through a sequence of Material-like shapes.
 *
 * This Compose implementation uses crossfade and subtle scale/rotation to suggest morphing
 * without relying on yet-to-land platform widgets, keeping it dependency-light.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    contained: Boolean = true,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    size: Dp = 56.dp,
) {
    // Seven-shape loop: circle, rounded rect, diamond, triangle, capsule, hexagon, square
    val shapes = remember { listOf(0, 1, 2, 3, 4, 5, 6) }
    var index by remember { mutableStateOf(0) }
    var rotateDeg by remember { mutableStateOf(0f) }
    var scaleFactor by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            index = (index + 1) % shapes.size
            rotateDeg = (rotateDeg + 30f) % 360f
            scaleFactor = 0.92f
            delay(220)
            scaleFactor = 1f
            delay(220)
        }
    }

    val containerModifier = if (contained) {
        modifier
            .size(size)
            .background(containerColor, CircleShape)
    } else modifier.size(size)

    Box(containerModifier, contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = index,
            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(240)) },
            label = "expressiveLoader"
        ) { targetIndex ->
            Canvas(Modifier.size(size * 0.64f)) {
                val w = size.toPx() * 0.64f
                val h = w
                val left = (this.size.width - w) / 2f
                val top = (this.size.height - h) / 2f
                val center = Offset(left + w / 2f, top + h / 2f)

                rotate(rotateDeg, center) {
                    scale(scaleFactor, scaleFactor, center) {
                        when (targetIndex) {
                            0 -> {
                                // Circle
                                drawCircle(color = indicatorColor, radius = w / 2.4f, center = center)
                            }
                            1 -> {
                                // Rounded rect
                                drawRoundRect(
                                    color = indicatorColor,
                                    topLeft = Offset(left, top),
                                    size = Size(w, h),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.24f, h * 0.24f)
                                )
                            }
                            2 -> {
                                // Diamond (rotated square)
                                val p = Path().apply {
                                    moveTo(center.x, top)
                                    lineTo(left + w, center.y)
                                    lineTo(center.x, top + h)
                                    lineTo(left, center.y)
                                    close()
                                }
                                drawPath(p, indicatorColor)
                            }
                            3 -> {
                                // Triangle
                                val p = Path().apply {
                                    moveTo(center.x, top)
                                    lineTo(left + w, top + h)
                                    lineTo(left, top + h)
                                    close()
                                }
                                drawPath(p, indicatorColor)
                            }
                            4 -> {
                                // Capsule (pill)
                                drawRoundRect(
                                    color = indicatorColor,
                                    topLeft = Offset(left, top + h * 0.18f),
                                    size = Size(w, h * 0.64f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.32f, h * 0.32f)
                                )
                            }
                            5 -> {
                                // Hexagon
                                val p = Path().apply {
                                    val r = w / 2f
                                    val cx = center.x
                                    val cy = center.y
                                    for (i in 0..5) {
                                        val a = Math.toRadians((60.0 * i) - 30.0)
                                        val x = (cx + r * kotlin.math.cos(a)).toFloat()
                                        val y = (cy + r * kotlin.math.sin(a)).toFloat()
                                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                                    }
                                    close()
                                }
                                drawPath(p, indicatorColor)
                            }
                            else -> {
                                // Square
                                drawRect(
                                    color = indicatorColor,
                                    topLeft = Offset(left, top),
                                    size = Size(w, h)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}