package com.hocchu.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hocchu.mobile.ui.StudentApp
import com.hocchu.mobile.ui.VuiHocTheme
import com.hocchu.mobile.ui.AuthGate
import com.hocchu.mobile.auth.AuthViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val auth = ViewModelProvider(this)[AuthViewModel::class.java]
        setContent { VuiHocTheme { AuthGate(auth) } }
    }
}
