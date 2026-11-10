package com.hocchu.mobile.classroom

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

internal fun submissionData(assignment: SchoolAssignment, draft: AssignmentDraft): String {
    require(draft.ready(assignment.content))
    return when(val content = assignment.content) {
        is AssignmentContent.Quiz -> JSONObject().put("answers", JSONArray(draft.answers)).toString()
        is AssignmentContent.Writing -> JSONArray().apply {
            content.letters.forEach { letter -> put(JSONObject().put("letter", letter)
                .put("image", renderWritingImage(draft.drawings.getValue(letter)))) }
        }.toString().also { require(it.length <= 4_000_000) }
        AssignmentContent.Unsupported -> throw ClassroomFailure("Loại bài tập chưa được hỗ trợ.")
    }
}

private fun renderWritingImage(strokes: List<DraftStroke>): String {
    val side = 512
    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val grid = Paint().apply { color = Color.rgb(219, 234, 244); strokeWidth = 1f }
        for(i in 0..10) {
            val at = i * side / 10f
            canvas.drawLine(at, 0f, at, side.toFloat(), grid)
            canvas.drawLine(0f, at, side.toFloat(), at, grid)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(38, 62, 80); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        strokes.forEach { stroke ->
            paint.strokeWidth = (.08f + stroke.pen * .04f) * side / 10f
            val points = stroke.points
            if(points.size == 1) { paint.style = Paint.Style.FILL
                canvas.drawCircle(points[0].x * side, points[0].y * side, paint.strokeWidth / 2f, paint) }
            if(points.size > 1) { paint.style = Paint.Style.STROKE
                val path = Path().apply { moveTo(points[0].x * side, points[0].y * side)
                    points.drop(1).forEach { lineTo(it.x * side, it.y * side) } }
                canvas.drawPath(path, paint)
            }
        }
        val bytes = ByteArrayOutputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)); stream.toByteArray() }
        return "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    } finally { bitmap.recycle() }
}
