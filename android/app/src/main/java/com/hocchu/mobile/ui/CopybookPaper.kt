package com.hocchu.mobile.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hocchu.mobile.R
import kotlin.math.sqrt

/** Non-interactive paper, shared by the writing surface and the reference dialog. */
@Composable
internal fun CopybookPaper(
    form: CopybookForm,
    modifier: Modifier = Modifier,
    showStart: Boolean = true
) {
    val resources = LocalResources.current
    val typeface = remember(resources) { resources.getFont(R.font.handwriting) }
    val glyph = remember(typeface, form) { copybookGlyph(typeface, form) }
    val model = remember(glyph) { glyph.path.asComposePath() }
    val labelPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }
    Canvas(modifier.background(CopybookColors.paper)) {
        val cell = size.width / CopybookPage.squares
        val baseline = CopybookPage.baseline * cell
        val origin = CopybookPage.originX(glyph) * cell
        drawRect(PaleBlue.copy(alpha = .35f), Offset(0f, baseline - 2f * cell), Size(size.width, 2f * cell))
        for (line in 0..CopybookPage.squares) {
            val position = line * cell
            drawLine(CopybookColors.grid, Offset(position, 0f), Offset(position, size.height), .6.dp.toPx())
            val main = (line - CopybookPage.baseline.toInt()) % 5 == 0
            drawLine(if (main) CopybookColors.rule else CopybookColors.grid,
                Offset(0f, position), Offset(size.width, position), if (main) 1.2.dp.toPx() else .6.dp.toPx())
        }
        translate(origin, baseline) {
            scale(cell, cell, pivot = Offset.Zero) { drawPath(model, SkyDark.copy(alpha = .3f)) }
        }
        // Labels are diagram measurements; scale with the squares, not the OS text size.
        labelPaint.color = SkyDark.toArgb()
        labelPaint.textSize = cell * .32f
        for (number in 1..6) {
            val center = Offset(.38f * cell, baseline - (number - 1) * cell)
            drawCircle(CopybookColors.paper, .26f * cell, center)
            drawContext.canvas.nativeCanvas.drawText(number.toString(), center.x,
                center.y - (labelPaint.ascent() + labelPaint.descent()) / 2f, labelPaint)
        }
        // The brace measures the body, excluding accents and the dot on i.
        val top = baseline - form.above * cell
        val bottom = baseline + form.below * cell
        val brace = cell * 8.65f
        val braceColor = SkyDark.copy(alpha = .65f)
        drawLine(braceColor, Offset(brace, top), Offset(brace, bottom), .8.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(.12f * cell, .1f * cell)))
        for (y in listOf(top, bottom)) {
            drawLine(braceColor, Offset(brace - cell * .13f, y), Offset(brace + cell * .13f, y), 1.dp.toPx())
        }
        labelPaint.textAlign = Paint.Align.LEFT
        drawContext.canvas.nativeCanvas.drawText(form.heightLabel, brace + .22f * cell,
            (top + bottom) / 2f - (labelPaint.ascent() + labelPaint.descent()) / 2f, labelPaint)
        labelPaint.textAlign = Paint.Align.CENTER
        if (showStart) {
            val start = Offset(origin + glyph.start.x * cell, baseline + glyph.start.y * cell)
            val length = sqrt(form.direction.getDistanceSquared())
            val forward = form.direction / length
            val end = start + forward * (cell * .55f)
            val normal = Offset(-forward.y, forward.x)
            val marker = CopybookColors.start
            drawLine(Color.White, start, end, 3.5.dp.toPx())
            drawLine(marker, start, end, 1.8.dp.toPx())
            drawLine(marker, end, end - forward * (.17f * cell) + normal * (.12f * cell), 1.8.dp.toPx())
            drawLine(marker, end, end - forward * (.17f * cell) - normal * (.12f * cell), 1.8.dp.toPx())
            drawCircle(Color.White, cell * .15f, start)
            drawCircle(marker, cell * .095f, start)
        }
    }
}

internal object CopybookColors {
    val paper = Color(0xFFFFFDF7)
    val grid = Color(0xFFCFE1EE)
    val rule = Color(0xFF7AAEC9)
    val start = Color(0xFF267B45)
}

@Preview(widthDp = 360, heightDp = 360)
@Composable
private fun CopybookPreview() {
    CopybookPaper(CopybookForms.forLetter("g"), Modifier.fillMaxWidth().aspectRatio(1f))
}
