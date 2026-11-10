package com.hocchu.mobile.classroom

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Only the account-scoped preferences passed by the classroom model are used. */
class AssignmentDraftStore(private val prefs: SharedPreferences) {
    fun load(assignment: SchoolAssignment): AssignmentDraft { return try {
        val json = JSONObject(prefs.getString("assignment.${assignment.id}", null) ?: return AssignmentDraft())
        if(json.getString("content") != assignment.fingerprint) return AssignmentDraft()
        val answers = json.getJSONArray("answers").let { a -> (0 until a.length()).map { a.getString(it) } }
        val drawings = json.getJSONObject("drawings")
        val strokes = drawings.keys().asSequence().associateWith { letter ->
            val a = drawings.getJSONArray(letter)
            (0 until a.length()).map { i -> val o = a.getJSONObject(i)
                val points = o.getJSONArray("points")
                DraftStroke((0 until points.length()).map { n -> val p = points.getJSONArray(n)
                    DraftPoint(p.getDouble(0).toFloat(), p.getDouble(1).toFloat()).also {
                        require(it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f) }
                }, o.getInt("pen").coerceIn(0, 2)) }
        }
        AssignmentDraft(answers, strokes, json.optInt("letterIndex").coerceAtLeast(0))
    } catch (_: Exception) { AssignmentDraft() } }
    fun save(assignment: SchoolAssignment, draft: AssignmentDraft) {
        val drawings = JSONObject()
        draft.drawings.forEach { (letter, strokes) ->
            drawings.put(letter, JSONArray().apply { strokes.forEach { stroke ->
                put(JSONObject().put("pen", stroke.pen).put("points", JSONArray().apply {
                    stroke.points.forEach { put(JSONArray().put(it.x.toDouble()).put(it.y.toDouble())) }
                })) }
            })
        }
        val data = JSONObject().put("content", assignment.fingerprint).put("answers", JSONArray(draft.answers))
            .put("drawings", drawings).put("letterIndex", draft.letterIndex).toString()
        if (!prefs.edit().putString("assignment.${assignment.id}", data).commit()) throw ClassroomFailure("Chưa lưu được bài đang làm trên máy.")
    }
    fun clear(id: Long) {
        if(!prefs.edit().remove("assignment.$id").commit()) throw ClassroomFailure("Bài đã gửi, nhưng chưa dọn được bản nháp trên máy.")
    }
}
