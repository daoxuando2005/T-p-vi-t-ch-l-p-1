package com.hocchu.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** Decorative companions share the app's original mascot and toy-like palette. */
@Composable
fun JourneyDecoration(scene: Int, modifier: Modifier = Modifier) {
    Box(modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension / 100f
            fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
            fun ellipse(color: Color, x: Float, y: Float, width: Float, height: Float) {
                drawOval(color, point(x, y), Size(width * unit, height * unit))
            }
            drawCircle(PaleBlue.copy(alpha = .65f), 44 * unit, point(50f, 50f))
            when (scene % 4) {
                0 -> {
                    // Soft clouds and a sun behind the owl.
                    drawCircle(Gold, 12 * unit, point(78f, 21f))
                    ellipse(Color.White, 6f, 22f, 35f, 15f)
                    drawCircle(Color.White, 10 * unit, point(22f, 22f))
                }
                1 -> {
                    ellipse(Color(0xFFCFEDB8), 8f, 82f, 84f, 10f)
                    drawLine(Color(0xFFBD8755), point(50f, 42f), point(50f, 84f), 10 * unit, StrokeCap.Round)
                    drawCircle(Color(0xFF82D75D), 23 * unit, point(35f, 39f))
                    drawCircle(Color(0xFF63C947), 24 * unit, point(64f, 39f))
                    drawCircle(Color(0xFF93DF65), 23 * unit, point(50f, 23f))
                    for (x in listOf(35f, 58f, 69f)) drawCircle(Coral, 4 * unit, point(x, 38f))
                }
                2 -> {
                    val colors = listOf(Sky, Orange, Color(0xFF9BD96A))
                    colors.forEachIndexed { index, color ->
                        val x = 24f + index * 26f
                        val y = if (index == 1) 12f else 24f
                        val string = Path().apply {
                            moveTo(x * unit, (y + 32) * unit)
                            quadraticTo((x - 8) * unit, 70 * unit, 50 * unit, 90 * unit)
                        }
                        drawPath(string, Muted.copy(alpha = .6f), style = Stroke(1.4f * unit))
                        ellipse(color, x - 13, y, 26f, 34f)
                        ellipse(Color.White.copy(alpha = .65f), x - 8, y + 5, 5f, 10f)
                    }
                }
                3 -> {
                    drawLine(SuccessText, point(50f, 40f), point(50f, 88f), 4 * unit, StrokeCap.Round)
                    ellipse(Color(0xFF86CB62), 29f, 67f, 21f, 11f)
                    ellipse(Color(0xFFA4D978), 50f, 58f, 22f, 12f)
                    for (angle in 0 until 6) {
                        val radians = angle * Math.PI / 3
                        val center = point(50f + kotlin.math.cos(radians).toFloat() * 17,
                            37f + kotlin.math.sin(radians).toFloat() * 17)
                        drawCircle(Color(0xFFFFB4AC), 12 * unit, center)
                    }
                    drawCircle(Gold, 12 * unit, point(50f, 37f))
                    drawCircle(Ink, 1.7f * unit, point(46f, 35f))
                    drawCircle(Ink, 1.7f * unit, point(54f, 35f))
                    drawArc(Ink, 0f, 180f, false, point(46f, 38f), Size(8 * unit, 5 * unit),
                        style = Stroke(1.5f * unit, cap = StrokeCap.Round))
                }
            }
        }
        if (scene % 4 == 0) Mascot(Modifier.size(64.dp))
    }
}
