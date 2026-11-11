package com.hocchu.mobile

import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.hocchu.mobile.auth.AuthState
import com.hocchu.mobile.auth.AuthSession
import com.hocchu.mobile.auth.UserAccount
import com.hocchu.mobile.auth.accountPreferences
import com.hocchu.mobile.data.LessonProgress
import com.hocchu.mobile.data.LessonProgressStore
import com.hocchu.mobile.ui.AuthContent
import com.hocchu.mobile.ui.LoginScreen
import com.hocchu.mobile.ui.VuiHocTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class LoginScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun requiredFieldsPasswordVisibilityLoadingAndServerErrors() {
        val state = mutableStateOf(AuthState.Login())
        var attempts = 0
        compose.runOnUiThread {
            compose.activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews()
            compose.activity.setContent { VuiHocTheme { LoginScreen(state.value, onSignIn = { username, password ->
                assertEquals("student", username); assertEquals("test password", password)
                attempts++; state.value = AuthState.Login(submitting = true)
            }) } }
        }
        compose.onNodeWithTag("login-submit").performScrollTo().performClick()
        compose.onNodeWithText("Nhập tên đăng nhập hoặc email").assertExists()
        assertEquals(0, attempts)
        compose.onNodeWithTag("login-username").performScrollTo().performTextInput(" student ")
        compose.onNodeWithTag("login-password").performScrollTo().performTextInput("test password")
        compose.onNodeWithContentDescription("Hiện mật khẩu").performClick()
        compose.onNodeWithContentDescription("Ẩn mật khẩu").assertExists()
        compose.onNodeWithTag("login-submit").performScrollTo().performClick()
        assertEquals(1, attempts)
        compose.onNodeWithText("Đang đăng nhập…").assertExists()
        compose.onNodeWithTag("login-username").assertIsNotEnabled()
        compose.runOnUiThread { state.value = AuthState.Login("Thông tin chưa đúng") }
        compose.onNodeWithTag("login-error").assertTextEquals("Thông tin chưa đúng")
    }

    @Test fun switchingAccountsDoesNotRestoreAnotherStudentsJourneyOrDraft() {
        val server = "https://test-${UUID.randomUUID()}.invalid/"
        val nameA = accountPreferences(server, 1)
        val nameB = accountPreferences(server, 2)
        val storeA = LessonProgressStore(compose.activity.getSharedPreferences(nameA, 0))
        val storeB = LessonProgressStore(compose.activity.getSharedPreferences(nameB, 0))
        storeA.save(0, LessonProgress(true, 2, 1))
        fun session(id: Long) = AuthSession(server, "not-used-by-ui", UserAccount(id, "student-$id", "Học sinh $id", setOf("ROLE_STUDENT")), Long.MAX_VALUE)
        val state = mutableStateOf<AuthState>(AuthState.SignedIn(session(1)))
        try {
            compose.runOnUiThread {
                compose.activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews()
                compose.activity.setContent { VuiHocTheme { AuthContent(state.value, server,
                    signIn = { username, _ -> state.value = AuthState.SignedIn(session(if(username == "student-b") 2 else 1)) },
                    retry = {}, logout = { state.value = AuthState.Login() }, changeServer = {}) } }
            }
            compose.onNodeWithTag("journey-continue").performClick()
            compose.onNodeWithText("Từ 2 / 3", substring = true).assertIsDisplayed()
            fun logout() {
                compose.onNodeWithContentDescription("Hồ sơ").performClick()
                compose.onNodeWithText("Đăng xuất").performClick()
                compose.onNodeWithTag("login-username").assertExists()
            }
            fun login(username: String) {
                compose.onNodeWithTag("login-username").performScrollTo().performTextInput(username)
                compose.onNodeWithTag("login-password").performScrollTo().performTextInput("test password")
                compose.onNodeWithTag("login-submit").performScrollTo().performClick()
            }
            logout(); login("student-b")
            compose.onNodeWithTag("journey-continue").performClick()
            compose.onNodeWithText("Chặng 1 / 4 · Chữ A").assertIsDisplayed()
            assertEquals(LessonProgress(), storeB.readAll()[0])
            logout(); login("student-a")
            compose.onNodeWithTag("journey-continue").performClick()
            compose.onNodeWithText("Từ 2 / 3", substring = true).assertIsDisplayed()
            assertEquals(LessonProgress(true, 2, 1), storeA.readAll()[0])
        } finally {
            compose.activity.deleteSharedPreferences(nameA)
            compose.activity.deleteSharedPreferences(nameB)
        }
    }
}
