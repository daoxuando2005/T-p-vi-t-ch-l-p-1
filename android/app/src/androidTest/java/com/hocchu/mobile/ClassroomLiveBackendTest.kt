package com.hocchu.mobile

import androidx.test.platform.app.InstrumentationRegistry
import com.hocchu.mobile.auth.*
import com.hocchu.mobile.classroom.*
import com.hocchu.mobile.family.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Requires disposable accounts supplied by AndroidJourneyLiveTest, which owns DB cleanup. */
class ClassroomLiveBackendTest {
    @Test fun nativeStudentJoinsSubmitsThreeTypesAndParentReceivesTeacherFeedback() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Live backend is opt-in", args.getString("liveBackend") == "true")
        fun argument(name: String) = requireNotNull(args.getString(name)) { "Disposable fixture argument missing: $name" }
        val server = normalizeServer(argument("backendUrl"), true)
        val transport = UrlAuthTransport(8 * 1024 * 1024)
        val auth = AuthApi(transport)
        val student = auth.me(auth.signIn(server, argument("studentUsername"), argument("studentPassword")))
        val parent = auth.me(auth.signIn(server, argument("parentUsername"), argument("parentPassword")))
        val signIn = transport.request(server + "api/auth/signin", "POST", null,
            JSONObject().put("usernameOrEmail",argument("teacherUsername")).put("password",argument("teacherPassword")).toString())
        assertEquals(200, signIn.status)
        val teacher = JSONObject(signIn.body).getString("accessToken")
        fun teacherRequest(path: String, method: String = "GET", body: JSONObject? = null): String {
            val reply = transport.request(server + "api/teacher/" + path, method, teacher, body?.toString())
            assertEquals("Teacher request failed: $method ${path.substringBefore('?')}", 200, reply.status)
            return reply.body
        }
        val school = JSONObject(teacherRequest("class?className=" + URLEncoder.encode("TEST-ANDROID-LIVE-${UUID.randomUUID()}","UTF-8"),"POST"))
        val classId = school.getLong("id")
        fun create(type: String, content: String): Long = JSONObject(teacherRequest("assignments","POST",
            JSONObject().put("classId",classId).put("title","TEST Android $type").put("contentType",type).put("contentData",content))).getLong("id")
        val writingId = create("WRITING", "{\"letters\":[\"A\"]}")
        val quizId = create("QUIZ", "[{\"question\":\"b□\",\"options\":[\"a\",\"o\"],\"answer\":\"a\"}]")
        val sentenceId = create("SENTENCE", "{\"sentences\":[{\"words\":[\"Bé\",\"yêu\",\"mẹ.\"]}]}")
        val api = ClassroomApi(transport)
        api.join(student, school.getString("inviteCode"))
        assertTrue(api.classes(student).any { it.id == classId })
        val initial = api.assignments(student).filter { it.id in setOf(writingId,quizId,sentenceId) }
        assertEquals(3, initial.size); assertTrue(initial.none { it.submitted })
        val assignment = initial.single { it.id == writingId }
        val draft = AssignmentDraft(drawings = mapOf("A" to listOf(DraftStroke(
            listOf(DraftPoint(.25f,.8f),DraftPoint(.5f,.2f),DraftPoint(.75f,.8f)), 1))))
        api.submit(student, writingId, submissionData(assignment,draft))
        val writing = api.review(student, writingId)
        assertEquals("AUTO", writing.gradeSource); assertNotNull(writing.autoScore)
        assertEquals(writing.autoScore,writing.score); assertTrue(writing.images.isNotEmpty()); assertTrue(writing.writingRatings.isNotEmpty())
        teacherRequest("submissions/${writing.id}/grade","POST",JSONObject().put("score",8.5).put("feedback","Cô đã nhận bài viết từ Android"))
        api.submit(student,quizId,"{\"answers\":[\"a\"]}")
        api.submit(student,sentenceId,"{\"sentences\":[[0,1,2]]}")
        assertEquals(10.0, api.review(student,quizId).score!!,.001)
        assertEquals(10.0, api.review(student,sentenceId).score!!,.001)
        assertTrue(api.assignments(student).filter {it.id in setOf(writingId,quizId,sentenceId)}.all {it.submitted})
        val family = FamilyApi(transport)
        family.link(parent, family.linkCode(student).code)
        assertTrue(family.children(parent).any { it.id == student.account.id })
        val reviewed = family.review(parent,student.account.id,writing.id)
        assertEquals(8.5, reviewed.score!!,.001); assertEquals("TEACHER",reviewed.gradeSource)
        assertEquals(writing.autoScore,reviewed.autoScore); assertEquals("Cô đã nhận bài viết từ Android",reviewed.feedback)
        val id = UUID.randomUUID().toString()
        val events = JSONArray().put(JSONObject().put("id",id).put("kind","READING").put("letter","A").put("source","FREE")
            .put("review",false).put("occurredAt",Instant.now().toString()))
        val progress = JSONArray()
        for(letter in com.hocchu.mobile.data.AlphabetContent.lessons) progress.put(JSONObject().put("letter",letter.letter)
            .put("read",letter.letter == "A").put("writes",0).put("words",0).put("sentences",0))
        val batch = JSONObject().put("events",events).put("progress",progress)
        assertEquals(setOf(id),family.sync(student,batch)); family.sync(student,batch)
        val dashboard = family.dashboard(parent,student.account.id,LocalDate.now(SCHOOL_ZONE))
        assertEquals(1,dashboard.readings); assertEquals(3,dashboard.submissions)
        assertTrue(family.progress(student).first().read)
        family.unlink(parent,student.account.id)
        assertTrue(family.children(parent).isEmpty())
        try { family.dashboard(parent,student.account.id,LocalDate.now(SCHOOL_ZONE)); fail("Unlinked parent cannot retain access") }
        catch(_: FamilyFailure) { }
        api.leave(student,classId)
        assertTrue(api.classes(student).none {it.id == classId})
    }
}
