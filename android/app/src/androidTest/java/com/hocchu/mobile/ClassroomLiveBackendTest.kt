package com.hocchu.mobile

import androidx.test.platform.app.InstrumentationRegistry
import com.hocchu.mobile.auth.*
import com.hocchu.mobile.classroom.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.URLEncoder
import java.util.UUID

class ClassroomLiveBackendTest {
    @Test fun nativeStudentJoinsSubmitsRealWritingAndReceivesTeacherFeedback() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Live backend is opt-in", args.getString("liveBackend") == "true")
        val server = normalizeServer(args.getString("backendUrl") ?: BuildConfig.DEFAULT_API_BASE_URL, true)
        val transport = UrlAuthTransport(8 * 1024 * 1024)
        val auth = AuthApi(transport)
        val student = auth.me(auth.signIn(server, "student1", "123456"))
        val signIn = transport.request(server + "api/auth/signin", "POST", null,
            JSONObject().put("usernameOrEmail","teacher1").put("password","123456").toString())
        assertEquals(200, signIn.status)
        val teacher = JSONObject(signIn.body).getString("accessToken")
        fun teacherRequest(path: String, method: String = "GET", body: JSONObject? = null): String {
            val reply = transport.request(server + "api/teacher/" + path, method, teacher, body?.toString())
            assertEquals("Teacher request failed: $method ${path.substringBefore('?')}", 200, reply.status)
            return reply.body
        }
        val school = JSONObject(teacherRequest("class?className=" + URLEncoder.encode("TEST-ANDROID-LIVE-${UUID.randomUUID()}","UTF-8"),"POST"))
        val classId = school.getLong("id")
        try {
            val item = JSONObject(teacherRequest("assignments","POST", JSONObject().put("classId",classId)
                .put("title","TEST bài viết Android").put("contentType","WRITING")
                .put("contentData",JSONObject().put("letters",JSONArray(listOf("A"))).toString())))
            val api = ClassroomApi(transport)
            api.join(student, school.getString("inviteCode"))
            assertTrue(api.classes(student).any { it.id == classId })
            val assignment = api.assignments(student).single { it.id == item.getLong("id") }
            val draft = AssignmentDraft(drawings = mapOf("A" to listOf(DraftStroke(
                listOf(DraftPoint(.25f,.8f),DraftPoint(.5f,.2f),DraftPoint(.75f,.8f)), 1))))
            api.submit(student, assignment.id, submissionData(assignment,draft))
            val rows = JSONArray(teacherRequest("assignments/${assignment.id}/submissions"))
            assertEquals(1, rows.length())
            val submission = rows.getJSONObject(0)
            assertTrue(submission.isNull("score"))
            val image = JSONArray(submission.getString("submissionData")).getJSONObject(0).getString("image")
            assertTrue(image.startsWith("data:image/png;base64,"))
            teacherRequest("submissions/${submission.getLong("id")}/grade","POST",
                JSONObject().put("score",8.5).put("feedback","Cô đã nhận bài viết từ Android"))
            val updated = api.assignments(student).single { it.id == assignment.id }
            assertTrue(updated.submitted)
            assertEquals(8.5, updated.score!!, .001)
            assertEquals("Cô đã nhận bài viết từ Android", updated.feedback)
        } finally { teacherRequest("class/$classId","DELETE") }
    }
}
