package com.hocchu.mobile

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.hocchu.mobile.auth.*
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in integration smoke test. Never replace a pre-existing saved user session. */
class AuthLiveLoginTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun loginRestoreInAFreshViewModelAndLogoutAgainstRealBackend() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveBackend") == "true")
        val prefs = compose.activity.getSharedPreferences("auth.session", 0)
        assumeTrue("Existing user session is preserved", !prefs.contains("sealed.v1"))
        var submitted = false
        val store = ViewModelStore()
        try {
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("login-username").fetchSemanticsNodes().isNotEmpty() }
            capture("login-screen.png")
            compose.onNodeWithTag("login-username").performScrollTo().performTextInput("student1")
            compose.onNodeWithTag("login-password").performScrollTo().performTextInput("123456")
            submitted = true
            compose.onNodeWithTag("login-submit").performScrollTo().performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("journey-continue").fetchSemanticsNodes().isNotEmpty() }
            assertTrue(prefs.contains("sealed.v1"))
            capture("authenticated-home.png")
            lateinit var restored: AuthViewModel
            compose.runOnUiThread {
                restored = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory.getInstance(compose.activity.application))[AuthViewModel::class.java]
            }
            compose.waitUntil(15_000) { restored.state is AuthState.SignedIn }
            assertEquals("student1", (restored.state as AuthState.SignedIn).session.account.username)
            compose.runOnUiThread { store.clear() }
            compose.onNodeWithContentDescription("Hồ sơ").performClick()
            compose.onNodeWithText("Đăng xuất").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("login-username").fetchSemanticsNodes().isNotEmpty() }
            assertFalse(prefs.contains("sealed.v1"))
        } finally {
            compose.runOnUiThread { store.clear() }
            // Only this explicitly-created demo session; never clear learning preferences.
            if (submitted) SessionVault(prefs).clear()
        }
    }
    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.filesDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
