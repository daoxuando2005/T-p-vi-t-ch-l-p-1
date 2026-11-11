package com.hocchu.mobile.family

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

data class LinkedChild(val id: Long, val name: String, val username: String)
data class ChildActivity(val id: String, val kind: String, val letter: String, val title: String,
    val occurredAt: Instant, val score: Double?, val feedback: String)
data class LetterStatus(val letter: String, val read: Boolean, val writes: Int, val words: Int, val sentences: Int, val complete: Boolean)
data class DayCount(val date: LocalDate, val activities: Int)
data class ParentDashboard(val studentId: Long, val studentName: String, val date: LocalDate,
    val readings: Int, val writings: Int, val exercises: Int, val submissions: Int,
    val timeline: List<ChildActivity>, val progress: List<LetterStatus>, val week: List<DayCount>,
    val completedLetters: Int, val progressAvailable: Boolean, val truncated: Boolean)
data class ParentState(val loading: Boolean = false, val linking: Boolean = false,
    val children: List<LinkedChild> = emptyList(), val selectedId: Long? = null,
    val date: LocalDate = LocalDate.now(SCHOOL_ZONE), val dashboard: ParentDashboard? = null,
    val error: String? = null, val notice: String? = null, val refreshedAt: Instant? = null)
data class LinkCode(val linked: Boolean, val code: String, val expiresAt: String, val parentName: String)
class FamilyFailure(val publicMessage: String) : Exception(publicMessage)
val SCHOOL_ZONE: java.time.ZoneId = java.time.ZoneId.of("Asia/Ho_Chi_Minh")
internal fun JSONObject.text(key: String) = if(isNull(key)) "" else optString(key)
