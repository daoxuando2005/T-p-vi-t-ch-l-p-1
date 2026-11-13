package com.hocchu.mobile

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.hocchu.mobile.auth.*
import com.hocchu.mobile.ui.RegisterScreen
import com.hocchu.mobile.ui.VuiHocTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RegistrationScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun mismatchBlocksSubmitAndParentRegistrationKeepsExactPassword() {
        val state = mutableStateOf(AuthState.Register())
        var submitted: RegistrationInput? = null
        compose.setContent { VuiHocTheme { RegisterScreen(state.value, { submitted = it; state.value = AuthState.Register(submitting = true) }, {}) } }
        compose.onNodeWithTag("register-role-PARENT").performScrollTo().performClick()
        for((tag,value) in listOf("fullName" to " Bé học ", "username" to " student ", "email" to "student@example.com", "password" to " secret ", "confirmation" to "different"))
            compose.onNodeWithTag("register-$tag").performScrollTo().performTextInput(value)
        compose.onNodeWithTag("register-submit").performScrollTo().performClick()
        compose.onNodeWithText("Hai mật khẩu chưa khớp").assertExists(); assertNull(submitted)
        compose.onNodeWithTag("register-confirmation").performScrollTo().performTextReplacement(" secret ")
        compose.onNodeWithTag("register-submit").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("PARENT", submitted?.role); assertEquals(" secret ", submitted?.password) }
        compose.onNodeWithTag("register-submit").assertIsNotEnabled()
        compose.onNodeWithTag("register-username").performScrollTo().assertIsNotEnabled()
    }
    @Test fun backReturnsToLoginAndServerErrorStaysReadable() {
        var back = false
        compose.setContent { VuiHocTheme { RegisterScreen(AuthState.Register("Tên đăng nhập đã tồn tại"), {}, { back = true }) } }
        compose.onNodeWithTag("register-error").performScrollTo().assertTextEquals("Tên đăng nhập đã tồn tại")
        compose.onNodeWithText("Đã có tài khoản? Đăng nhập").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(back) }
    }
}
