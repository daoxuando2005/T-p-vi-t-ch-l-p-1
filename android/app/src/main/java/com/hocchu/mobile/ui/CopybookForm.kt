package com.hocchu.mobile.ui

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import java.util.Locale

/** Medium-size Vietnamese school handwriting. One body unit is TWO small squares.
 * See docs/COPYBOOK_VI.md for references, rendering rules and review boundaries.
 * The baseline is y = 0; negative y is above the writing line. */
internal data class CopybookForm(
    val letter: String,
    val above: Float = 2f,
    val below: Float = 0f,
    val reference: String = letter,
    val start: Offset = Offset(1.42f, -1.82f),
    val direction: Offset = Offset(-1f, -.35f),
    val parts: List<LetterPart>
) {
    val height: Float get() = above + below
    val hasExtraMark: Boolean get() = reference != letter
    val heightLabel: String get() = "${height.li()} li"
    val caption: String get() = when {
        below > 0f -> "${height.li()} li: ${above.li()} li trên, ${below.li()} li dưới dòng"
        hasExtraMark -> "Thân chữ ${above.li()} li · dấu ở phía trên"
        else -> "Cao ${above.li()} li · viết trên dòng kẻ đậm"
    }
}

internal enum class LetterPart(val label: String) {
    ClosedCurve("Cong kín"), RightHook("Móc ngược phải"), Breve("Dấu trăng"),
    HatUp("Nét xiên lên của dấu mũ"), HatDown("Nét xiên xuống của dấu mũ"),
    UpperLoop("Khuyết trên"), LowerLoop("Khuyết dưới"), LeftCurve("Cong trái"),
    SmallTwist("Vòng xoắn nhỏ"), CurvedLoop("Nét cong tạo vòng khuyết"),
    Entry("Nét hất"), Dot("Chấm trên đầu chữ"), DoubleHook("Móc hai đầu"),
    ForwardHook("Móc xuôi"), Bar("Nét ngang"), Horn("Dấu móc"),
    Downstroke("Nét sổ thẳng"), Rising("Nét xiên lên"), RightCurve("Cong phải"),
    TwistedHook("Móc hai đầu có vòng xoắn ở giữa")
}

private fun Float.li(): String = if (this % 1f == 0f) toInt().toString() else toString().replace('.', ',')

internal object CopybookForms {
    private val loopStart = Offset(0f, -1f)
    private val upward = Offset(1f, -1f)
    private fun parts(vararg value: LetterPart) = value.toList()
    private val aParts = parts(LetterPart.ClosedCurve, LetterPart.RightHook)
    private val forms = listOf(
        CopybookForm("a", parts = aParts),
        CopybookForm("ă", reference = "a", parts = aParts + LetterPart.Breve),
        CopybookForm("â", reference = "a", parts = aParts + parts(LetterPart.HatUp, LetterPart.HatDown)),
        CopybookForm("b", 5f, start = loopStart, direction = upward,
            parts = parts(LetterPart.UpperLoop, LetterPart.RightHook, LetterPart.SmallTwist)),
        CopybookForm("c", parts = parts(LetterPart.LeftCurve)),
        CopybookForm("d", 4f, parts = aParts),
        CopybookForm("đ", 4f, parts = aParts + LetterPart.Bar),
        CopybookForm("e", start = Offset(-.15f, -.2f), direction = Offset(1f, -.4f),
            parts = parts(LetterPart.CurvedLoop)),
        CopybookForm("ê", reference = "e", start = Offset(-.15f, -.2f), direction = Offset(1f, -.4f),
            parts = parts(LetterPart.CurvedLoop, LetterPart.HatUp, LetterPart.HatDown)),
        CopybookForm("g", 2f, 3f, parts = parts(LetterPart.ClosedCurve, LetterPart.LowerLoop)),
        CopybookForm("h", 5f, start = loopStart, direction = upward,
            parts = parts(LetterPart.UpperLoop, LetterPart.DoubleHook)),
        CopybookForm("i", reference = "u", start = loopStart, direction = upward,
            parts = parts(LetterPart.Entry, LetterPart.RightHook, LetterPart.Dot)),
        CopybookForm("k", 5f, start = loopStart, direction = upward,
            parts = parts(LetterPart.UpperLoop, LetterPart.TwistedHook)),
        CopybookForm("l", 5f, start = loopStart, direction = upward,
            parts = parts(LetterPart.UpperLoop, LetterPart.RightHook)),
        CopybookForm("m", start = Offset(0f, -1.5f), direction = upward,
            parts = parts(LetterPart.ForwardHook, LetterPart.ForwardHook, LetterPart.DoubleHook)),
        CopybookForm("n", start = Offset(0f, -1.5f), direction = upward,
            parts = parts(LetterPart.ForwardHook, LetterPart.DoubleHook)),
        CopybookForm("o", parts = parts(LetterPart.ClosedCurve)),
        CopybookForm("ô", reference = "o", parts = parts(LetterPart.ClosedCurve, LetterPart.HatUp, LetterPart.HatDown)),
        CopybookForm("ơ", reference = "o", parts = parts(LetterPart.ClosedCurve, LetterPart.Horn)),
        CopybookForm("p", 2f, 2f, start = loopStart, direction = upward,
            parts = parts(LetterPart.Entry, LetterPart.Downstroke, LetterPart.DoubleHook)),
        CopybookForm("q", 2f, 2f, parts = parts(LetterPart.ClosedCurve, LetterPart.Downstroke)),
        CopybookForm("r", 2.5f, start = Offset(-.65f, -.04f), direction = upward,
            parts = parts(LetterPart.Rising, LetterPart.SmallTwist, LetterPart.DoubleHook)),
        CopybookForm("s", 2.5f, start = Offset(-.1f, -.05f), direction = upward,
            parts = parts(LetterPart.Rising, LetterPart.SmallTwist, LetterPart.RightCurve)),
        CopybookForm("t", 3f, start = loopStart, direction = upward,
            parts = parts(LetterPart.Entry, LetterPart.RightHook, LetterPart.Bar)),
        CopybookForm("u", start = loopStart, direction = upward,
            parts = parts(LetterPart.Entry, LetterPart.RightHook, LetterPart.RightHook)),
        CopybookForm("ư", reference = "u", start = loopStart, direction = upward,
            parts = parts(LetterPart.Entry, LetterPart.RightHook, LetterPart.RightHook, LetterPart.Horn)),
        CopybookForm("v", start = Offset(-.15f, -1.5f), direction = Offset(1f, -.15f),
            parts = parts(LetterPart.DoubleHook, LetterPart.SmallTwist)),
        CopybookForm("x", start = Offset(.1f, -1.8f), direction = Offset(1f, -.2f),
            parts = parts(LetterPart.RightCurve, LetterPart.LeftCurve)),
        CopybookForm("y", 2f, 3f, start = loopStart, direction = upward,
            parts = parts(LetterPart.Entry, LetterPart.RightHook, LetterPart.LowerLoop))
    ).associateBy { it.letter }

    val all: Collection<CopybookForm> get() = forms.values
    fun forLetter(letter: String): CopybookForm = forms.getValue(letter.lowercase(Locale.ROOT))
}

internal data class CopybookGlyph(val path: Path, val bounds: RectF, val start: Offset)

/** Use the actual outline, never Text's line box, to align the model with the grid.
 * Upper and lower halves share the baseline but are measured independently: a
 * descender must not shrink the oval of g/q, and accents must not shrink a/o/u. */
internal fun copybookGlyph(typeface: Typeface, form: CopybookForm): CopybookGlyph {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = 2048f
    }
    fun outline(text: String) = Path().also { paint.getTextPath(text, 0, text.length, 0f, 0f, it) }
    val source = outline(form.letter)
    val referenceBounds = outline(form.reference).tightBounds()
    val sourceBounds = source.tightBounds()
    val unit = -outline("a").tightBounds().top / 2f
    val upperScale = form.above / -referenceBounds.top
    // Path.op can return even-odd contours. addPath does not copy the fill type;
    // preserving parity keeps the counters in e/g/h/k/l open after merging halves.
    val result = Path().apply { fillType = Path.FillType.EVEN_ODD }
    fun addHalf(top: Float, bottom: Float, scaleY: Float) {
        val half = Path(source)
        val clip = Path().apply { addRect(-8192f, top, 8192f, bottom, Path.Direction.CW) }
        check(half.op(clip, Path.Op.INTERSECT))
        half.transform(Matrix().apply { setScale(1f / unit, scaleY) })
        result.addPath(half)
    }
    addHalf(-8192f, 0f, upperScale)
    if (form.below > 0f) addHalf(0f, 8192f, form.below / sourceBounds.bottom)
    if (form.letter == "r") {
        // HP001's r starts above the baseline. Add the introductory rising stroke
        // so the starting cue does not incorrectly teach starting halfway up r.
        val contours = result.approximate(.002f)
        val joinTarget = Offset(-.15f, -1f)
        val joinIndex = (contours.indices step 3).minBy { i ->
            (Offset(contours[i + 1], contours[i + 2]) - joinTarget).getDistanceSquared()
        }
        val join = Offset(contours[joinIndex + 1], contours[joinIndex + 2])
        val entry = Path().apply {
            moveTo(form.start.x, form.start.y)
            lineTo(join.x, join.y)
        }
        val fill = Path()
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = .07f
            strokeCap = Paint.Cap.ROUND
            getFillPath(entry, fill)
        }
        check(result.op(fill, Path.Op.UNION))
    }
    val points = result.approximate(.002f)
    // Snap the pedagogical starting location onto the model's actual contour.
    // This marker is a placement cue; outline traversal is NOT stroke order.
    var start = form.start
    var distance = Float.POSITIVE_INFINITY
    for (i in points.indices step 3) {
        val point = Offset(points[i + 1], points[i + 2])
        val candidate = (point - form.start).getDistanceSquared()
        if (candidate < distance) { distance = candidate; start = point }
    }
    return CopybookGlyph(result, result.tightBounds(), start)
}

/** Path.computeBounds includes Bezier control points, so it overestimates loops.
 * Sampling here is cached per letter and independent of screen size/font scale. */
internal fun Path.tightBounds(): RectF {
    val values = approximate(.01f)
    require(values.isNotEmpty()) { "Missing handwriting glyph" }
    val bounds = RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
    for (i in values.indices step 3) {
        bounds.left = minOf(bounds.left, values[i + 1]); bounds.right = maxOf(bounds.right, values[i + 1])
        bounds.top = minOf(bounds.top, values[i + 2]); bounds.bottom = maxOf(bounds.bottom, values[i + 2])
    }
    return bounds
}

/** A fixed coordinate system keeps the grid, model and saved ink in register. */
internal object CopybookPage {
    const val squares = 10
    const val baseline = 6f
    const val center = 5.2f
    fun originX(glyph: CopybookGlyph) = center - glyph.bounds.centerX()
}
