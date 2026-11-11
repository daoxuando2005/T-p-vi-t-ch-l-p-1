package com.hocchu.mobile

import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.hocchu.mobile.family.*
import com.hocchu.mobile.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ParentScreenTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun emptyFamilyCanOpenCodeFormAndLogout() {
        var sent="";var loggedOut=false
        compose.runOnUiThread {
            compose.activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews()
            compose.activity.setContent {VuiHocTheme {ParentScreen("Phụ huynh thử",ParentState(),{},{},{},{sent=it},{loggedOut=true})}}
        }
        compose.onNodeWithTag("parent-start-link").performClick()
        compose.onNodeWithTag("parent-feed").performScrollToNode(hasTestTag("parent-link-submit"))
        compose.onNodeWithTag("parent-link-submit").assertIsNotEnabled()
        compose.onNodeWithTag("parent-link-code").performTextInput("AbCdEfGhIjKlMnOpQr-_12")
        compose.onNodeWithTag("parent-link-submit").performClick()
        assertEquals("AbCdEfGhIjKlMnOpQr-_12",sent)
        compose.onNodeWithTag("parent-profile").performClick()
        compose.onNodeWithTag("parent-logout").performClick();assertTrue(loggedOut)
    }
}
