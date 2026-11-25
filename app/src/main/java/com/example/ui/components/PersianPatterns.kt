package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a subtle Persian octagonal geometric Girih/Shamseh tilework lattice
 * inspired by the 8-sided plan and turquoise-gold tilework of Soltaniyeh Dome.
 */
@Composable
fun PersianTileworkBackdrop(
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White.copy(alpha = 0.09f),
    accentColor: Color = Color(0xFFE0B862).copy(alpha = 0.16f)
) {
    Canvas(modifier = modifier) {
        val spacing = 58.dp.toPx()
        val radius = spacing * 0.42f
        val cols = (size.width / spacing).toInt() + 2
        val rows = (size.height / spacing).toInt() + 2

        for (row in -1..rows) {
            for (col in -1..cols) {
                val offsetX = if (row % 2 == 0) 0f else spacing * 0.5f
                val cx = col * spacing + offsetX
                val cy = row * spacing

                val starPath = Path()
                val innerRadius = radius * 0.52f
                val points = 16
                for (i in 0 until points) {
                    val angle = (i * (2.0 * PI / points)) - PI / 2.0
                    val r = if (i % 2 == 0) radius else innerRadius
                    val x = cx + (r * cos(angle)).toFloat()
                    val y = cy + (r * sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                }
                starPath.close()

                drawPath(
                    path = starPath,
                    color = lineColor,
                    style = Stroke(width = 1.2.dp.toPx())
                )

                drawCircle(
                    color = accentColor,
                    radius = 2.2.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
fun PersianGoldDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFC49232).copy(alpha = 0.45f)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val cy = size.height / 2f
        val cx = size.width / 2f
        val diamondHalf = 5.dp.toPx()
        val gap = 14.dp.toPx()

        drawLine(
            color = color,
            start = Offset(0f, cy),
            end = Offset(cx - gap, cy),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(cx + gap, cy),
            end = Offset(size.width, cy),
            strokeWidth = 1.dp.toPx()
        )

        val diamond = Path().apply {
            moveTo(cx, cy - diamondHalf)
            lineTo(cx + diamondHalf, cy)
            lineTo(cx, cy + diamondHalf)
            lineTo(cx - diamondHalf, cy)
            close()
        }
        drawPath(diamond, color = color)
    }
}
