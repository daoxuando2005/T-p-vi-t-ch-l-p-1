package com.hocchu.mobile

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.hocchu.mobile.ui.StudentApp
import com.hocchu.mobile.ui.VuiHocTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentExperienceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun showStudentFixture() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            activity.deleteSharedPreferences("test.student.ui")
            activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews()
            activity.setContent { VuiHocTheme { StudentApp(preferencesName = "test.student.ui") } }
        }
    }

    @Test
    fun writingKeepsToolsAndDraftAfterRecreation() {
        val restoration = StateRestorationTester(compose)
        compose.runOnUiThread { compose.activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews() }
        restoration.setContent { VuiHocTheme { StudentApp(preferencesName = "test.student.ui") } }
        compose.onNodeWithTag("destination-2").performClick()
        compose.onNodeWithText("Con viết xong").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Bảng ô ly tập viết chữ a").performTouchInput {
            swipe(Offset(width * .25f, height * .3f), Offset(width * .7f, height * .7f))
        }
        compose.onNodeWithText("Con viết xong").assertIsEnabled()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Con viết xong").assertIsEnabled().assertIsDisplayed()
        compose.onNodeWithText("Hoàn tác").assertIsDisplayed()
        compose.onNodeWithText("B").performClick()
        compose.onNodeWithText("Đổi chữ luyện viết?").assertIsDisplayed()
        compose.onNodeWithText("Viết tiếp").performClick()
        compose.onNodeWithText("Con viết xong").assertIsEnabled()
        capture("writing-portrait.png")
        compose.onNodeWithText("Hoàn tác").performClick()
        compose.onNodeWithText("Con viết xong").assertIsNotEnabled()
    }

    @Test
    fun landscapeKeepsBoardAndToolsVisible() {
        try {
            compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            compose.waitUntil(10_000) {
                compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            showStudentFixture()
            compose.onNodeWithTag("destination-2").performClick()
            compose.onNodeWithTag("destination-3").assertIsDisplayed()
            compose.onNodeWithContentDescription("Bảng ô ly tập viết chữ a").assertIsDisplayed()
            compose.onNodeWithText("Hoàn tác").assertIsDisplayed()
            compose.onNodeWithText("Con viết xong").assertIsDisplayed()
            capture("writing-landscape.png")
        } finally {
            compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    @Test
    fun classroomDoesNotShowDemoAssignments() {
        compose.onNodeWithTag("destination-3").performClick()
        compose.onNodeWithText("Lớp 1A").assertDoesNotExist()
        compose.onNodeWithText("Bé chưa tham gia lớp nào. Nhập mã cô giáo cung cấp để nhận bài tập nhé.").assertIsDisplayed()
    }

    private fun capture(name: String, dialog: Boolean = false) {
        val node = if (dialog) compose.onNode(isDialog()) else compose.onRoot()
        val image = node.captureToImage().asAndroidBitmap()
        File(compose.activity.filesDir, name).outputStream().use { output ->
            image.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @Test
    fun readingOpensTheSameLetterAndCompletionCanRepeatOrAdvance() {
        compose.onNodeWithTag("destination-1").performClick()
        compose.onNodeWithTag("letter-picker").performScrollToIndex(4)
        compose.onNodeWithText("C", substring = false).performClick()
        compose.onNodeWithTag("destination-0").performClick()
        capture("journey.png")
        compose.onNodeWithTag("destination-1").performClick()
        compose.onNodeWithText("Tập viết chữ C").performScrollTo().performClick()
        drawLetter("c")
        finishWriting()
        compose.onNodeWithText("Bé đã rất chăm chỉ!").assertIsDisplayed()
        compose.onNodeWithText("Độ bám mẫu",substring=true).assertExists()
        capture("writing-completion.png", dialog = true)
        compose.onNodeWithText("Viết lại").performClick()
        compose.onNodeWithText("Con viết xong").assertIsNotEnabled()
        drawLetter("c")
        finishWriting()
        compose.onNodeWithText("Tiếp tục").performClick()
        compose.onNodeWithContentDescription("Bảng ô ly tập viết chữ d").assertIsDisplayed()
        compose.onNodeWithText("Con viết xong").assertIsNotEnabled()
        compose.onNodeWithTag("destination-1").performClick()
        compose.onNodeWithText("Tập viết chữ C").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Bảng ô ly tập viết chữ c").assertIsDisplayed()
    }

    @Test
    fun finishingTheLastLetterCanRestartTheAlphabet() {
        compose.onNodeWithTag("destination-2").performClick()
        compose.onNodeWithTag("letter-picker").performScrollToIndex(28)
        compose.onNodeWithText("Y", substring = false).performClick()
        drawLetter("y")
        finishWriting()
        compose.onNodeWithText("Luyện từ chữ A").performClick()
        compose.onNodeWithContentDescription("Bảng ô ly tập viết chữ a").assertIsDisplayed()
        compose.onNodeWithText("Con viết xong").assertIsNotEnabled()
    }

    private fun drawLetter(letter: String) {
        compose.onNodeWithContentDescription("Bảng ô ly tập viết chữ $letter").performTouchInput {
            swipe(Offset(width * .25f, height * .3f), Offset(width * .7f, height * .7f))
        }
    }

    private fun finishWriting() {
        compose.onNodeWithText("Con viết xong").performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("Độ bám mẫu",substring=true).fetchSemanticsNodes().isNotEmpty()}
    }

    @Test
    fun copybookReferenceKeepsTheDraftAndShowsLetterSpecificGuidance() {
        compose.onNodeWithTag("destination-2").performClick()
        compose.onNodeWithContentDescription("Xem cách viết chữ a").performClick()
        compose.onNodeWithText("Cùng viết chữ a").assertIsDisplayed()
        compose.onNodeWithText("Con hiểu rồi").performClick()
        compose.onNodeWithText("Con viết xong").assertIsNotEnabled()
        drawLetter("a")
        compose.onNodeWithContentDescription("Xem cách viết chữ a").performClick()
        compose.onNodeWithText("Con hiểu rồi").performClick()
        compose.onNodeWithText("Con viết xong").assertIsEnabled()
        compose.onNodeWithText("Hoàn tác").performClick()
        // Inspect representative models without losing or inadvertently adding ink.
        for ((index, letter) in listOf(2 to "â", 9 to "g", 10 to "h", 21 to "r")) {
            compose.onNodeWithTag("letter-picker").performScrollToIndex(index)
            compose.onNodeWithText(letter.uppercase(), substring = false).performClick()
            compose.onNodeWithContentDescription("Bảng ô ly tập viết chữ $letter").assertIsDisplayed()
            compose.onNodeWithText("Con viết xong").assertIsNotEnabled()
            capture("copybook-$letter.png")
        }
        compose.onNodeWithContentDescription("Xem cách viết chữ r").performClick()
        capture("copybook-reference.png", dialog = true)
        compose.onNodeWithText("Con hiểu rồi").performClick()
    }
}
