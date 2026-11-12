package com.hocchu.mobile

import android.app.Application
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.geometry.Offset
import com.hocchu.mobile.auth.*
import com.hocchu.mobile.classroom.*
import com.hocchu.mobile.ui.ClassroomScreen
import com.hocchu.mobile.ui.VuiHocTheme
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ClassroomScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun allThreeSentenceRowsMustUseAllTilesBeforeSubmitting() {
        val user=AuthSession("https://sentence-ui-${UUID.randomUUID()}.test/","fixture.token.value",
            UserAccount(7009,"fixture","Bé thử",setOf("ROLE_STUDENT")),System.currentTimeMillis()+600000)
        val content="""{"sentences":[{"words":["Bé","yêu","mẹ."]},{"words":["Ba","đọc","sách."]},{"words":["Bé","đi","học."]}]}"""
        var posted=false
        val api=ClassroomApi(AuthTransport {url,_,_,body-> when {
            url.endsWith("class/joined")->HttpReply(200,"""[{"id":12,"name":"Lớp ghép câu"}]""")
            url.endsWith("assignments")->HttpReply(200,JSONArray().put(JSONObject().put("id",19).put("classId",12).put("title","Ghép câu của bé")
                .put("contentType","SENTENCE").put("contentData",content).put("isSubmitted",posted)).toString())
            url.endsWith("/submit")-> {val rows=JSONObject(JSONObject(body!!).getString("submissionData")).getJSONArray("sentences")
                assertEquals(3,rows.length());for(i in 0..2)assertEquals(listOf(0,1,2),(0..2).map {rows.getJSONArray(i).getInt(it)})
                posted=true;HttpReply(200,"""{"success":true}""")}
            else->error("Unexpected request")
        }})
        val model=ClassroomViewModel(compose.activity.application as Application,api)
        try {
            compose.activityRule.scenario.onActivity {activity->
                activity.enableEdgeToEdge();activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews();model.connect(user) {}
                activity.setContent {VuiHocTheme {ClassroomScreen(model) {}}}
            }
            compose.waitUntil(10000) {!model.state.loading}
            compose.onNodeWithTag("assignment-group-Sentence").performScrollTo().performClick()
            compose.onNodeWithTag("assignment-19").performScrollTo().performClick()
            compose.waitUntil(10000) {!model.state.opening}
            for(i in 0..2) {
                compose.onNodeWithTag("submit-assignment").assertIsNotEnabled()
                for(tile in 0..2)compose.onNodeWithTag("sentence-$i-$tile").performScrollTo().performClick()
            }
            compose.onNodeWithTag("submit-assignment").assertIsEnabled().performClick()
            compose.onNodeWithTag("confirm-submit").performClick()
            compose.waitUntil(10000) {posted&&!model.state.submitting&&model.state.selectedId==null}
        }finally {
            compose.runOnIdle {model.connect(null) {}}
            compose.activity.deleteSharedPreferences(accountPreferences(user.server,user.account.id)+".classroom")
        }
    }
    @Test fun groupAndClassBadgesOnlyCountUnsubmittedWorkInThatClass() {
        val user=AuthSession("https://class-badges-${UUID.randomUUID()}.test/","fixture.token.value",
            UserAccount(7003,"fixture","Bé thử",setOf("ROLE_STUDENT")),System.currentTimeMillis()+600000)
        val items=JSONArray()
        for((id,school,type,done) in listOf(listOf(1,10,"WRITING",false),listOf(2,20,"QUIZ",false),listOf(3,20,"QUIZ",true))) {
            items.put(JSONObject().put("id",id).put("classId",school).put("title","Bài $id").put("contentType",type)
                .put("contentData",if(type=="WRITING") "A" else "[{\"question\":\"Chọn A\",\"options\":[\"A\",\"B\"],\"answer\":\"A\"}]")
                .put("isSubmitted",done))
        }
        val api=ClassroomApi(AuthTransport {url,_,_,_->HttpReply(200,if(url.endsWith("class/joined"))
            "[{\"id\":10,\"name\":\"Lớp A\"},{\"id\":20,\"name\":\"Lớp B\"}]" else items.toString())})
        val model=ClassroomViewModel(compose.activity.application as Application,api)
        try {
            compose.activityRule.scenario.onActivity {activity->
                activity.enableEdgeToEdge();activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews()
                model.connect(user) {};activity.setContent {VuiHocTheme {ClassroomScreen(model) {}}}
            }
            compose.waitUntil(10000) {!model.state.loading}
            compose.onNodeWithTag("class-pending-20",useUnmergedTree=true).assertContentDescriptionEquals("1 bài chưa nộp")
            compose.onNodeWithTag("group-pending-Writing",useUnmergedTree=true).assertExists()
            compose.onNodeWithTag("group-pending-Quiz",useUnmergedTree=true).assertDoesNotExist()
            java.io.File(compose.activity.filesDir,"classroom-groups-proof.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
            }
            compose.onNodeWithTag("class-20").performScrollTo().performClick()
            compose.onNodeWithTag("group-pending-Writing",useUnmergedTree=true).assertDoesNotExist()
            compose.onNodeWithTag("group-pending-Quiz",useUnmergedTree=true).assertContentDescriptionEquals("1 bài chưa nộp")
            compose.onNodeWithTag("assignment-group-Quiz").performScrollTo().performClick()
            compose.onNodeWithText("Bài 2").assertExists()
            compose.onNodeWithText("Bài 1").assertDoesNotExist()
            compose.onNodeWithTag("assignment-groups-back").performScrollTo().performClick()
            compose.onNodeWithTag("assignment-group-Sentence").assertExists()
        } finally {compose.runOnIdle {model.connect(null) {}}}
    }
    @Test fun writingRequiresAllLettersAndKeepsInkWhenChangingLetter() {
        val user = AuthSession("https://writing-ui-${UUID.randomUUID()}.test/", "fixture.token.value",
            UserAccount(7002,"fixture","Bé thử",setOf("ROLE_STUDENT")),System.currentTimeMillis()+600000)
        val api = ClassroomApi(AuthTransport { url, _, _, _ -> when {
            url.endsWith("class/joined") -> HttpReply(200,"[{\"id\":12,\"name\":\"Lớp thử\",\"teacherName\":\"Cô thử\"}]")
            url.endsWith("assignments") -> HttpReply(200, JSONArray().put(JSONObject().put("id",16).put("classId",12)
                .put("title","Viết A và B").put("contentType","WRITING").put("isSubmitted",false)
                .put("contentData","{\"letters\":[\"A\",\"B\"]}")).toString())
            else -> error("Unexpected request")
        } })
        val app = compose.activity.application as Application
        val model = ClassroomViewModel(app,api)
        try {
            compose.activityRule.scenario.onActivity { activity ->
                activity.enableEdgeToEdge();activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews()
                model.connect(user) {}
                activity.setContent { VuiHocTheme { ClassroomScreen(model) {} } }
            }
            compose.waitUntil(10000) { model.state.assignments.isNotEmpty() }
            compose.onNodeWithTag("assignment-group-Writing").performScrollTo().performClick()
            compose.onNodeWithTag("assignment-16").performScrollTo().performClick()
            compose.waitUntil(10000) { !model.state.opening }
            compose.onNodeWithTag("submit-assignment").assertIsNotEnabled()
            compose.onNodeWithTag("class-writing-board").performTouchInput {
                swipe(Offset(width*.2f,height*.8f),Offset(width*.6f,height*.3f),400)
            }
            compose.waitUntil(5000) { model.state.draft.drawings["A"].orEmpty().isNotEmpty() }
            compose.onNodeWithText("Tiếp").performClick()
            compose.onNodeWithTag("submit-assignment").assertIsNotEnabled()
            compose.onNodeWithTag("class-writing-board").performTouchInput {
                swipe(Offset(width*.3f,height*.2f),Offset(width*.6f,height*.8f),400)
            }
            compose.onNodeWithTag("submit-assignment").assertIsEnabled()
            compose.onNodeWithText("Trước").performClick()
            assertTrue(model.state.draft.drawings.getValue("A").isNotEmpty())
            val image = compose.onRoot().captureToImage().asAndroidBitmap()
            java.io.File(app.filesDir,"classroom-writing-proof.png").outputStream().use {
                image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        } finally {
            compose.runOnIdle { model.connect(null) {} }
            app.deleteSharedPreferences(accountPreferences(user.server,user.account.id)+".classroom")
        }
    }
    @Test fun joinsClassCompletesQuizAndDoesNotShowSuccessForFailedSubmission() {
        var joined = false
        var failSubmit = true
        var posted = false
        val user = AuthSession("https://classroom-ui-${UUID.randomUUID()}.test/", "fixture.token.value",
            UserAccount(7001,"fixture","Bé thử",setOf("ROLE_STUDENT")),System.currentTimeMillis()+600000)
        val api = ClassroomApi(AuthTransport { url, _, _, body -> when {
            url.contains("class/join?") -> { joined = true; HttpReply(200,"{\"success\":true}") }
            url.endsWith("class/joined") -> HttpReply(200, if(joined) "[{\"id\":12,\"name\":\"Lớp Sao Nhỏ\",\"teacherName\":\"Cô Lan\"}]" else "[]")
            url.endsWith("assignments") -> HttpReply(200, if(!joined) "[]" else JSONArray().put(JSONObject()
                .put("id",15).put("classId",12).put("title","Chọn chữ cô giao").put("contentType","QUIZ")
                .put("contentData","[{\"question\":\"Chọn chữ A\",\"options\":[\"A\",\"B\"],\"answer\":\"A\"}]")
                .put("isSubmitted",posted).put("score",if(posted) 10 else JSONObject.NULL)).toString())
            url.endsWith("/submit") -> {
                assertEquals("A", JSONObject(JSONObject(body!!).getString("submissionData")).getJSONArray("answers").getString(0))
                if(failSubmit) HttpReply(500, "") else { posted = true; HttpReply(200,"{\"success\":true}") }
            }
            else -> error("Unexpected request")
        } })
        val app = compose.activity.application as Application
        val model = ClassroomViewModel(app, api)
        try {
            compose.activityRule.scenario.onActivity { activity ->
                activity.enableEdgeToEdge();activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews()
                model.connect(user) {}
                activity.setContent { VuiHocTheme { ClassroomScreen(model) {} } }
            }
            compose.waitUntil(10000) { !model.state.loading }
            compose.onNodeWithTag("join-expand").performClick()
            compose.onNodeWithTag("class-code").performTextInput("SAONHO")
            compose.onNodeWithTag("class-join").performScrollTo().performClick()
            compose.waitUntil(10000) { model.state.assignments.isNotEmpty() }
            compose.onNodeWithText("Lớp Sao Nhỏ").assertExists()
            compose.onNodeWithTag("group-pending-Quiz",useUnmergedTree=true).assertExists()
            compose.onNodeWithTag("assignment-group-Quiz").performScrollTo().performClick()
            compose.onNodeWithTag("assignment-15").performScrollTo().performClick()
            compose.waitUntil(10000) { !model.state.opening }
            compose.onNodeWithTag("submit-assignment").assertIsNotEnabled()
            compose.onNodeWithTag("answer-0-0").performScrollTo().performClick()
            compose.onNodeWithTag("submit-assignment").performClick()
            compose.onNodeWithTag("confirm-submit").performClick()
            compose.waitUntil(10000) { !model.state.submitting && model.state.error != null }
            compose.onNodeWithTag("submission-error").assertIsDisplayed()
            assertEquals(listOf("A"), model.state.draft.answers)
            failSubmit = false
            compose.onNodeWithTag("submit-assignment").performClick()
            compose.onNodeWithTag("confirm-submit").performClick()
            compose.waitUntil(10000) { posted && !model.state.loading && model.state.selectedId == null }
            compose.onNodeWithText("Điểm: 10.0 / 10").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("assignment-groups-back").performScrollTo().performClick()
            compose.onNodeWithTag("group-pending-Quiz",useUnmergedTree=true).assertDoesNotExist()
            compose.onNodeWithTag("assignment-group-Sentence").performScrollTo().performClick()
            compose.onNodeWithText("Lớp chưa có bài tập ở mục này.").assertExists()
        } finally {
            compose.runOnIdle { model.connect(null) {} }
            app.deleteSharedPreferences(accountPreferences(user.server,user.account.id)+".classroom")
        }
    }
}
