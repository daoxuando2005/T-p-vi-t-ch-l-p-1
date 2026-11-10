package com.hocchu.mobile.classroom

import com.hocchu.mobile.data.DemoContent
import org.json.JSONArray
import org.json.JSONObject

data class SchoolClass(val id: Long, val name: String, val teacher: String)
data class QuizQuestion(val text: String, val options: List<String>, val spoken: String)
sealed interface AssignmentContent {
    data class Quiz(val questions: List<QuizQuestion>) : AssignmentContent
    data class Writing(val letters: List<String>) : AssignmentContent
    data object Unsupported : AssignmentContent
}
data class SchoolAssignment(val id: Long, val classId: Long, val className: String, val title: String,
    val description: String, val type: String, val rawContent: String, val dueDate: String,
    val submitted: Boolean, val score: Double?, val feedback: String,
    val content: AssignmentContent) {
    val fingerprint: String get() = java.security.MessageDigest.getInstance("SHA-256")
        .digest("$type:$rawContent".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
}
data class DraftPoint(val x: Float, val y: Float)
data class DraftStroke(val points: List<DraftPoint>, val pen: Int)
data class AssignmentDraft(val answers: List<String> = emptyList(),
    val drawings: Map<String, List<DraftStroke>> = emptyMap(), val letterIndex: Int = 0) {
    fun ready(content: AssignmentContent): Boolean = when(content) {
        is AssignmentContent.Quiz -> answers.size == content.questions.size &&
            answers.indices.all { answers[it] in content.questions[it].options }
        is AssignmentContent.Writing -> content.letters.all { letter ->
            drawings[letter].orEmpty().any { it.points.isNotEmpty() } }
        AssignmentContent.Unsupported -> false
    }
}
data class ClassroomState(val loading: Boolean = false, val joining: Boolean = false,
    val submitting: Boolean = false, val opening: Boolean = false,
    val classes: List<SchoolClass> = emptyList(), val assignments: List<SchoolAssignment> = emptyList(),
    val classId: Long? = null, val selectedId: Long? = null, val draft: AssignmentDraft = AssignmentDraft(),
    val error: String? = null, val notice: String? = null) {
    val selected: SchoolAssignment? get() = assignments.find { it.id == selectedId }
}
class ClassroomFailure(val publicMessage: String) : Exception(publicMessage)

internal object ClassroomJson {
    fun classes(json: String): List<SchoolClass> = JSONArray(json).let { array ->
        (0 until array.length()).map { val o = array.getJSONObject(it)
            SchoolClass(positive(o, "id"), o.getString("name"), text(o, "teacherName").ifBlank { "Giáo viên" }) }
    }
    fun assignments(json: String): List<SchoolAssignment> = JSONArray(json).let { array ->
        (0 until array.length()).map { val o = array.getJSONObject(it)
            val type = o.getString("contentType")
            val raw = o.optString("contentData")
            SchoolAssignment(positive(o, "id"), positive(o, "classId"), o.optString("className"),
                o.getString("title"), text(o, "description"), type, raw, text(o, "dueDate"),
                o.optBoolean("isSubmitted"), if(o.isNull("score")) null else o.getDouble("score"),
                text(o, "feedback"), content(type, raw)) }
    }
    fun content(type: String, raw: String): AssignmentContent = try {
        when(type.uppercase()) {
            "QUIZ" -> {
                val a = JSONArray(raw)
                require(a.length() in 1..100)
                val questions = (0 until a.length()).map {
                    val q = a.getJSONObject(it)
                    val aOptions = q.getJSONArray("options")
                    val options = (0 until aOptions.length()).map { n -> aOptions.getString(n) }
                    require(options.size in 2..8 && options.all(String::isNotBlank) && options.distinct().size == options.size)
                    QuizQuestion(q.optString("question").ifBlank { "Chọn đáp án đúng" }, options,
                        text(q, "animalName").ifBlank { q.optString("question") })
                }
                AssignmentContent.Quiz(questions)
            }
            "WRITING" -> {
                val text = raw.trim()
                val letters = if(text.startsWith("{")) {
                    val o = JSONObject(text)
                    o.optJSONArray("letters")?.let { a -> (0 until a.length()).map { a.getString(it) } }
                        ?: listOf(o.getString("letter"))
                } else listOf(text)
                val normalized = letters.map { it.trim().uppercase(java.util.Locale.ROOT) }.distinct()
                require(normalized.isNotEmpty() && normalized.all { letter -> DemoContent.lessons.any { it.letter == letter } })
                AssignmentContent.Writing(normalized)
            }
            else -> AssignmentContent.Unsupported
        }
    } catch (_: Exception) { AssignmentContent.Unsupported }
    private fun positive(o: JSONObject, key: String) = o.getLong(key).also { require(it > 0) }
    private fun text(o: JSONObject, key: String) = if(o.isNull(key)) "" else o.optString(key)
}
