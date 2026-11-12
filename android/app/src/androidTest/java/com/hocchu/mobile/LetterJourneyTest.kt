package com.hocchu.mobile

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.hocchu.mobile.data.*
import com.hocchu.mobile.ui.StudentApp
import com.hocchu.mobile.ui.VuiHocTheme
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Isolated preferences: tests must never erase or advance the child's real journey. */
class LetterJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var prefs: SharedPreferences
    private lateinit var store: LessonProgressStore
    private lateinit var prefsName: String
    @Before fun prepare() {
        compose.activityRule.scenario.onActivity {it.enableEdgeToEdge()}
        prefsName = "journey-test-${UUID.randomUUID()}"
        prefs = compose.activity.getSharedPreferences(prefsName, 0)
        store = LessonProgressStore(prefs)
    }
    @After fun cleanup() { compose.activity.deleteSharedPreferences(prefsName) }
    private fun start() {
        compose.runOnUiThread {
            // Replace only this test Activity's composition; the injected store is isolated.
            compose.activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews()
            compose.activity.setContent { VuiHocTheme { StudentApp(store) } }
        }
    }
    private fun openLetter(index: Int) {
        compose.onNodeWithTag("journey-list").performScrollToIndex(index + 2)
        compose.onNodeWithTag("journey-letter-$index").performClick()
    }
    private fun click(text: String) {
        compose.waitUntil(10000) {compose.onAllNodesWithText(text,substring=false).fetchSemanticsNodes().isNotEmpty()}
        val node = compose.onNodeWithText(text, substring = false)
        if (compose.onAllNodes(hasText(text, substring = false) and hasAnyAncestor(hasScrollAction()))
                .fetchSemanticsNodes().isNotEmpty()) node.performScrollTo()
        node.performClick()
    }
    private fun draw() {
        compose.onNodeWithContentDescription("Bảng ô ly tập viết chữ a").performTouchInput {
            swipe(Offset(width * .35f, height * .4f), Offset(width * .6f, height * .6f))
        }
        click("Con viết xong")
    }
    private fun capture(name: String) {
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.filesDir, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun fullLessonChecksAnswersResumesUnlocksAndReviewsWithoutErasingProgress() {
        start()
        compose.onNodeWithTag("journey-list").performScrollToIndex(3)
        compose.onNodeWithTag("journey-letter-1").assertIsNotEnabled()
        openLetter(0)
        compose.onNodeWithText("Con đã đọc theo").performScrollTo().assertIsNotEnabled()
        click("Nghe chữ cái")
        if (compose.onAllNodesWithText("Đã hiểu").fetchSemanticsNodes().isNotEmpty()) click("Đã hiểu")
        click("Con đã đọc theo")
        draw()
        click("Viết lượt 2")
        assertEquals(1, store.readAll()[0].writes)
        compose.onNodeWithContentDescription("Quay lại").performClick()
        openLetter(0)
        compose.onNodeWithText("Lượt viết 2 / 2", substring = true).assertIsDisplayed()
        draw()
        click("Đố vui với từ")
        assertEquals(LessonStage.Words, store.readAll()[0].stage)
        compose.onNodeWithTag("word-option-b").performScrollTo().performClick()
        click("Kiểm tra")
        click("Thử lại")
        assertEquals(0, store.readAll()[0].words)
        for (question in 0..2) {
            compose.onNodeWithTag("word-option-a").performScrollTo().performClick()
            click("Kiểm tra")
            click("Tiếp tục")
        }
        assertEquals(LessonStage.Sentences, store.readAll()[0].stage)
        capture("journey-sentence.png")
        for (tile in listOf(1, 0, 2)) compose.onNodeWithTag("sentence-tile-$tile").performScrollTo().performClick()
        click("Kiểm tra"); click("Thử lại")
        assertEquals(0, store.readAll()[0].sentences)
        for (question in 0..2) {
            for (tile in 0..2) compose.onNodeWithTag("sentence-tile-$tile").performScrollTo().performClick()
            click("Kiểm tra"); click("Tiếp tục")
        }
        assertTrue(store.readAll()[0].complete)
        compose.onNodeWithText("Hoàn thành chữ A!").assertIsDisplayed()
        capture("journey-completed.png")
        click("Về hành trình")
        compose.onNodeWithTag("journey-list").performScrollToIndex(3)
        compose.onNodeWithTag("journey-letter-1").assertIsEnabled()
        capture("journey-unlocked.png")
        openLetter(0)
        compose.onNodeWithText("Ôn lại chữ A").assertIsDisplayed()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        assertTrue(store.readAll()[0].complete)
        openLetter(1)
        compose.onNodeWithText("Chặng 1 / 4 · Chữ Ă").assertIsDisplayed()
    }

    @Test fun persistedPartialLessonResumesAndOptionalWritingCannotSkipWords() {
        store.save(0, LessonProgress(true, 1))
        start(); openLetter(0)
        draw(); click("Luyện thêm một lượt")
        assertEquals(2, store.readAll()[0].writes)
        compose.onNodeWithText("Luyện thêm", substring = true).assertIsDisplayed()
        assertFalse(LessonProgress.unlocked(1, store.readAll()))
        compose.onNodeWithText("Đố vui với từ").performClick()
        compose.onNodeWithTag("word-option-a").performScrollTo().performClick()
        click("Kiểm tra"); click("Tiếp tục")
        compose.onNodeWithContentDescription("Quay lại").performClick()
        openLetter(0)
        compose.onNodeWithText("Từ 2 / 3", substring = true).assertIsDisplayed()
        // The optional round may be skipped; the two required rounds stay credited.
        // A new repository instance sees the saved checkpoint.
        val restored = LessonProgressStore(compose.activity.getSharedPreferences(prefsName, 0)).readAll()
        assertEquals(LessonStage.Words, restored[0].stage)
        assertEquals(1, restored[0].words)
        assertEquals(restored, store.readAll())
    }

    @Test fun finalLetterCompletesTheAlphabetWithoutOpeningANonexistentLesson() {
        for (i in 0..27) store.save(i, LessonProgress(true, 2, 3, 3))
        store.save(28, LessonProgress(true, 2, 3, 2))
        start(); openLetter(28)
        for (tile in LetterCurriculum.words(28)[2].sentence.indices)
            compose.onNodeWithTag("sentence-tile-$tile").performScrollTo().performClick()
        click("Kiểm tra"); click("Tiếp tục")
        assertTrue(store.readAll().all { it.complete })
        compose.onNodeWithText("Bé đã hoàn thành hành trình 29 chữ cái. Cùng ôn lại nhé!").assertIsDisplayed()
        click("Về hành trình")
        compose.onNodeWithTag("journey-list").performScrollToIndex(1)
        compose.onNodeWithText("29 / 29 chữ đã hoàn thành").performScrollTo().assertIsDisplayed()
    }

    @Test fun storeRejectsLockedLessonsAndPreservesEarnedProgress() {
        store.save(1, LessonProgress(true, 2, 3, 3))
        assertFalse(store.readAll()[1].read)
        store.save(0, LessonProgress(true, 2, 2))
        store.save(0, LessonProgress())
        assertEquals(LessonProgress(true, 2, 2), store.readAll()[0])
    }
}
