package com.hocchu.mobile

import android.app.Application
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.hocchu.mobile.auth.*
import com.hocchu.mobile.classroom.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ClassroomProtocolTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as Application
    private fun student(id: Long = 701) = AuthSession("https://classroom-${UUID.randomUUID()}.test/", "test.token.value",
        UserAccount(id, "fixture", "Học sinh thử", setOf("ROLE_STUDENT")), System.currentTimeMillis() + 600000)
    private fun assignment(type: String = "QUIZ", raw: String = "[{\"question\":\"Chọn A\",\"options\":[\"A\",\"B\"],\"answer\":\"A\"}]") =
        SchoolAssignment(15, 12, "Lớp thử", "Bài thử", "", type, raw, "", false, null, "", ClassroomJson.content(type, raw))

    @Test fun parsesTeacherQuizAndBothWritingFormatsWithoutPretendingToGrade() {
        assertTrue(assignment().content is AssignmentContent.Quiz)
        assertEquals(listOf("Ă"), (assignment("WRITING", "ă").content as AssignmentContent.Writing).letters)
        assertEquals(listOf("A", "B"), (assignment("WRITING", "{\"letters\":[\"A\",\"B\"],\"targetScore\":70}").content as AssignmentContent.Writing).letters)
        assertEquals(AssignmentContent.Unsupported, assignment("WRITING", "Z").content)
        assertEquals(AssignmentContent.Unsupported, assignment("QUIZ", "[]").content)
    }

    @Test fun usesBearerQueryEncodingAndRequiresServerAcknowledgement() {
        val user = student()
        var expected = "join"
        val api = ClassroomApi(AuthTransport { url, method, token, body ->
            assertEquals(user.token, token); assertEquals("POST", method)
            if(expected == "join") { assertTrue(url.endsWith("class/join?inviteCode=A%26B")); assertNull(body)
                HttpReply(200, "{\"success\":true}")
            } else { assertTrue(url.endsWith("assignments/15/submit"))
                assertEquals("{\"answers\":[\"A\"]}", JSONObject(body!!).getString("submissionData"))
                HttpReply(200, "{\"success\":false,\"message\":\"Bài chưa hợp lệ\"}") }
        })
        api.join(user, " A&B ")
        expected = "submit"
        assertThrows(ClassroomFailure::class.java) { api.submit(user, 15, "{\"answers\":[\"A\"]}") }
        assertThrows(AuthFailure.Expired::class.java) { ClassroomApi(AuthTransport { _, _, _, _ -> HttpReply(401, "") }).classes(user) }
        assertThrows(ClassroomFailure::class.java) { ClassroomApi(AuthTransport { _, _, _, _ -> HttpReply(403, "") }).classes(user) }
    }

    @Test fun sentenceDraftKeepsRepeatedTilesAndServerReviewKeepsGradeProvenance() {
        val item=assignment("SENTENCE","""{"sentences":[{"words":["bé","và","bé","học."]}]}""")
        val content=item.content as AssignmentContent.Sentence
        assertFalse(AssignmentDraft(sentences=listOf(listOf(0,1,0,3))).ready(content))
        assertFalse(AssignmentDraft(sentences=listOf(listOf(0,1,2))).ready(content))
        val draft=AssignmentDraft(sentences=listOf(listOf(2,1,0,3)))
        assertTrue(draft.ready(content))
        val name="sentence-fixture-${UUID.randomUUID()}"
        try {
            val store=AssignmentDraftStore(app.getSharedPreferences(name,0))
            store.save(item,draft);assertEquals(draft,store.load(item))
            assertEquals(listOf(2,1,0,3),(0..3).map {JSONObject(submissionData(item,draft)).getJSONArray("sentences").getJSONArray(0).getInt(it)})
            val review=parseSubmissionReview(JSONObject().put("id",8).put("studentId",701).put("title","Ghép câu").put("type","SENTENCE")
                .put("assignmentContent",item.rawContent).put("submissionData",submissionData(item,draft))
                .put("score",8.5).put("autoScore",10).put("gradeSource","TEACHER").toString())
            assertEquals("bé và bé học.",review.answers.single().selected)
            assertEquals(review.answers.single().expected,review.answers.single().selected)
            assertEquals(10.0,review.autoScore!!,0.0);assertEquals("TEACHER",review.gradeSource)
        } finally {app.deleteSharedPreferences(name)}
    }

    @Test fun draftSurvivesReloadIsIsolatedAndRejectsChangedAssignmentContent() {
        val user = student()
        val names = listOf(accountPreferences(user.server, 701) + ".classroom", accountPreferences(user.server, 702) + ".classroom")
        try {
            val a = AssignmentDraftStore(app.getSharedPreferences(names[0], 0))
            val b = AssignmentDraftStore(app.getSharedPreferences(names[1], 0))
            val item = assignment()
            a.save(item, AssignmentDraft(answers = listOf("B")))
            assertEquals(listOf("B"), a.load(item).answers)
            assertTrue(b.load(item).answers.isEmpty())
            assertTrue(a.load(item.copy(rawContent = item.rawContent + " ")).answers.isEmpty())
            a.clear(item.id); assertTrue(a.load(item).answers.isEmpty())
        } finally { names.forEach(app::deleteSharedPreferences) }
    }

    @Test fun writingSubmissionContainsRealPngAndNoFabricatedScore() {
        val item = assignment("WRITING", "A")
        val draft = AssignmentDraft(drawings = mapOf("A" to listOf(DraftStroke(listOf(DraftPoint(.2f,.8f),DraftPoint(.5f,.2f)), 1))))
        val data = JSONArray(submissionData(item, draft)).getJSONObject(0)
        assertEquals("A", data.getString("letter")); assertFalse(data.has("score"))
        val bytes = Base64.decode(data.getString("image").substringAfter(','), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull(bitmap); assertEquals(512, bitmap.width); bitmap.recycle()
    }

    @Test fun failedSubmitKeepsDraftAndRetryClearsItOnlyAfterAcknowledgement() {
        val user = student()
        val name = accountPreferences(user.server, user.account.id) + ".classroom"
        var fail = true
        var posted = false
        val item = assignment()
        val api = ClassroomApi(AuthTransport { url, _, _, _ -> when {
            url.endsWith("class/joined") -> HttpReply(200, "[{\"id\":12,\"name\":\"Lớp thử\",\"teacherName\":\"Cô thử\"}]")
            url.endsWith("assignments") -> HttpReply(200, JSONArray().put(JSONObject().put("id",15).put("classId",12)
                .put("title","Bài thử").put("contentType","QUIZ").put("contentData",item.rawContent)
                .put("isSubmitted",posted)).toString())
            url.endsWith("/submit") -> if(fail) HttpReply(500, "") else { posted = true; HttpReply(200, "{\"success\":true}") }
            else -> error("Unexpected request")
        } })
        lateinit var model: ClassroomViewModel
        main { model = ClassroomViewModel(app, api); model.connect(user) { error("Unexpected expiration") } }
        try {
            waitFor { !model.state.loading && model.state.assignments.isNotEmpty() }
            main { model.open(15) }; waitFor { !model.state.opening }
            main { model.answer(0, "A") }
            waitFor { app.getSharedPreferences(name,0).contains("assignment.15") }
            main { model.submit() }; waitFor { model.state.error != null && !model.state.submitting }
            assertEquals(listOf("A"), model.state.draft.answers)
            assertTrue(app.getSharedPreferences(name,0).contains("assignment.15"))
            fail = false
            main { model.submit() }; waitFor { model.state.selectedId == null && !model.state.loading && posted }
            assertFalse(app.getSharedPreferences(name,0).contains("assignment.15"))
            assertTrue(model.state.assignments.single().submitted)
            main { model.connect(null) {} }
            assertTrue(model.state.assignments.isEmpty())
        } finally { main { model.connect(null) {} }; app.deleteSharedPreferences(name) }
    }
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    @Test fun lateResponseFromPreviousStudentCannotReplaceNewStudentsClasses() {
        val old = student().copy(token = "old.token.value")
        val next = old.copy(token = "next.token.value", account = old.account.copy(id = 702))
        val started = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val api = ClassroomApi(AuthTransport { url, _, token, _ ->
            if(url.endsWith("class/joined")) {
                if(token == old.token) { started.countDown(); release.await(5,java.util.concurrent.TimeUnit.SECONDS) }
                val id = if(token == old.token) 11 else 22
                HttpReply(200,"[{\"id\":$id,\"name\":\"Lớp $id\"}]")
            } else HttpReply(200,"[]")
        })
        lateinit var model: ClassroomViewModel
        main { model = ClassroomViewModel(app,api); model.connect(old) {} }
        try {
            assertTrue(started.await(5,java.util.concurrent.TimeUnit.SECONDS))
            main { model.connect(next) {} }
            release.countDown()
            waitFor { !model.state.loading && model.state.classes.singleOrNull()?.id == 22L }
            assertEquals("Lớp 22",model.state.classes.single().name)
        } finally { release.countDown(); main { model.connect(null) {} } }
    }
    private fun waitFor(condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 10000
        while(System.currentTimeMillis() < until) {
            var done = false; main { done = condition() }; if(done) return; Thread.sleep(25)
        }
        fail("Timed out waiting for classroom state")
    }
}
