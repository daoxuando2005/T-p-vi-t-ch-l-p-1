package com.hocchu.mobile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hocchu.mobile.data.DemoContent
import com.hocchu.mobile.ui.CopybookForms
import com.hocchu.mobile.ui.CopybookPage
import com.hocchu.mobile.ui.copybookGlyph
import com.hocchu.mobile.ui.tightBounds
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CopybookFormTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val typeface get() = context.resources.getFont(R.font.handwriting)

    @Test
    fun bodyHeightUsesSquaresRatherThanFontLineHeight() {
        val groups = mapOf(
            2f to "acemnouvx", 2.5f to "rs", 3f to "t", 4f to "dđ", 5f to "bhkl"
        )
        groups.forEach { (height, letters) -> letters.forEach { letter ->
            val glyph = copybookGlyph(typeface, CopybookForms.forLetter(letter.toString()))
            assertEquals("height of $letter", height, -glyph.bounds.top, .015f)
            assertTrue("$letter must sit on the baseline", glyph.bounds.bottom in -.02f.. .02f)
        } }
    }

    @Test
    fun descendersKeepTwoSquaresAboveTheBaseline() {
        mapOf("g" to 3f, "y" to 3f, "p" to 2f, "q" to 2f).forEach { (letter, depth) ->
            val glyph = copybookGlyph(typeface, CopybookForms.forLetter(letter))
            assertEquals("body above baseline for $letter", 2f, -glyph.bounds.top, .015f)
            assertEquals("descender of $letter", depth, glyph.bounds.bottom, .015f)
        }
    }

    @Test
    fun accentsDoNotShrinkTheBodyAndIDotIsNotTheBodyHeight() {
        listOf("ă", "â", "ê", "ô", "ơ", "ư", "i").forEach { letter ->
            val glyph = copybookGlyph(typeface, CopybookForms.forLetter(letter))
            assertTrue("$letter needs room for its mark", glyph.bounds.top < -2.05f)
            val body = Path(glyph.path)
            val region = Path().apply { addRect(-20f, -2.01f, 20f, 0f, Path.Direction.CW) }
            assertTrue(body.op(region, Path.Op.INTERSECT))
            assertEquals("body of $letter remains two squares", 2f, -body.tightBounds().top, .025f)
        }
    }

    @Test
    fun closedCountersAndLoopsStayOpenAfterClippingTheOutline() {
        // Interior locations in li, relative to the glyph origin and baseline.
        // Regression: adding Path.op results to a WINDING path filled some loops.
        val counters = mapOf(
            "a" to (.75f to -1f), "o" to (.75f to -1f), "g" to (.75f to -1f),
            "e" to (.65f to -1.4f), "h" to (1f to -3.8f),
            "k" to (1f to -3.8f), "l" to (1f to -3.8f)
        )
        counters.forEach { (letter, point) ->
            val bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(50f, 160f)
            canvas.scale(24f, 24f)
            canvas.drawPath(copybookGlyph(typeface, CopybookForms.forLetter(letter)).path,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLUE })
            val pixel = bitmap.getPixel((50f + point.first * 24f).toInt(), (160f + point.second * 24f).toInt())
            assertEquals("open counter of $letter", 0, Color.alpha(pixel))
            bitmap.recycle()
        }
    }

    @Test
    fun all29ModelsFitThePaperAndHaveAStartingCueOnTheGlyph() {
        val letters = DemoContent.lessons.map { it.letter.lowercase() }.toSet()
        assertEquals(29, letters.size)
        assertEquals(letters, CopybookForms.all.map { it.letter }.toSet())
        CopybookForms.all.forEach { form ->
            val glyph = copybookGlyph(typeface, form)
            val left = CopybookPage.originX(glyph) + glyph.bounds.left
            val right = CopybookPage.originX(glyph) + glyph.bounds.right
            assertTrue("${form.letter} has a real outline", glyph.bounds.width() > .8f)
            assertTrue("${form.letter} clears line numbers and height brace", left > 1f && right < 8.4f)
            assertTrue("top margin for ${form.letter}", CopybookPage.baseline + glyph.bounds.top >= .9f)
            assertTrue("bottom margin for ${form.letter}", CopybookPage.baseline + glyph.bounds.bottom <= 9.1f)
            assertTrue(form.parts.isNotEmpty())
            val samples = glyph.path.approximate(.002f)
            val distance = (samples.indices step 3).minOf { i ->
                val dx = samples[i + 1] - glyph.start.x
                val dy = samples[i + 2] - glyph.start.y
                dx * dx + dy * dy
            }
            assertEquals("starting cue on ${form.letter}", 0f, distance, .001f)
        }
        saveAlphabetProof()
    }

    // A visual proof sheet for checking ALL models together, including accent/descender space.
    private fun saveAlphabetProof() {
        val tile = 240
        val heading = 28
        val bitmap = Bitmap.createBitmap(tile * 5, (tile + heading) * 6, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(255, 253, 247))
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        CopybookForms.all.forEachIndexed { index, form ->
            val glyph = copybookGlyph(typeface, form)
            canvas.save()
            canvas.translate((index % 5 * tile).toFloat(), (index / 5 * (tile + heading)).toFloat())
            paint.color = Color.DKGRAY; paint.textSize = 17f
            canvas.drawText("${form.letter} (${form.heightLabel})", 12f, 21f, paint)
            canvas.translate(0f, heading.toFloat())
            val cell = tile / 10f
            for (line in 0..10) {
                paint.color = if (line == 6) Color.rgb(90, 160, 190) else Color.rgb(218, 231, 239)
                paint.strokeWidth = if (line == 6) 1.5f else 1f
                canvas.drawLine(0f, line * cell, tile.toFloat(), line * cell, paint)
                canvas.drawLine(line * cell, 0f, line * cell, tile.toFloat(), paint)
            }
            canvas.translate(CopybookPage.originX(glyph) * cell, CopybookPage.baseline * cell)
            canvas.scale(cell, cell)
            paint.color = Color.rgb(0, 101, 144)
            canvas.drawPath(glyph.path, paint)
            paint.color = Color.rgb(38, 123, 69)
            canvas.drawCircle(glyph.start.x, glyph.start.y, .12f, paint)
            canvas.restore()
        }
        File(context.filesDir, "copybook-alphabet-proof.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
