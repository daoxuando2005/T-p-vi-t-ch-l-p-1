package com.hocchu.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hocchu.mobile.BuildConfig
import com.hocchu.mobile.auth.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hocchu.mobile.classroom.ClassroomViewModel
import com.hocchu.mobile.family.ParentViewModel
import com.hocchu.mobile.family.StudentActivityModel
import com.hocchu.mobile.data.*

@Composable
fun AuthGate(model: AuthViewModel) {
    val classroom: ClassroomViewModel = viewModel()
    val parent: ParentViewModel = viewModel()
    val activities: StudentActivityModel = viewModel()
    val study:StudyContentModel=viewModel()
    LaunchedEffect(model.state) {
        val session=(model.state as? AuthState.SignedIn)?.session
        val isParent=session?.account?.roles?.contains("ROLE_PARENT")==true
        classroom.connect(session?.takeUnless {isParent}) { model.logout(AuthFailure.Expired().publicMessage) }
        activities.connect(session?.takeUnless {isParent}) {model.logout(AuthFailure.Expired().publicMessage)}
        parent.connect(session?.takeIf {isParent}) {model.logout(AuthFailure.Expired().publicMessage)}
        study.connect(session?.takeUnless {isParent}) {model.logout(AuthFailure.Expired().publicMessage)}
    }
    AuthContent(if(activities.finishing) AuthState.Loading("Đang lưu hoạt động của con…") else model.state,
        model.server, model::signIn, model::restore, {activities.finishSession {model.logout()}}, model::changeServer, classroom,parent,activities,study)
}

@Composable
internal fun AuthContent(state: AuthState, server: String, signIn: (String, String) -> Unit,
                         retry: () -> Unit, logout: () -> Unit, changeServer: (String) -> Unit,
                         classroom: ClassroomViewModel? = null, parent: ParentViewModel? = null,
                         activities: StudentActivityModel? = null,study:StudyContentModel?=null) {
    var settings by remember { mutableStateOf(false) }
    when(state) {
        is AuthState.SignedIn -> {
            val session = state.session
            val scope = accountPreferences(session.server, session.account.id)
            key(scope) {
                CompositionLocalProvider(LocalStudyContent provides (study?.content?:StudyContent.bundled()),LocalMediaServer provides session.server) {
                if("ROLE_PARENT" in session.account.roles) ParentApp(session.account.fullName,parent,logout)
                else StudentApp(preferencesName = scope, studentName = session.account.fullName, onLogout = logout, classroom = classroom,activities=activities,study=study)
                }
            }
        }
        is AuthState.Login -> key(server) {
            LoginScreen(state, signIn, onSettings = if(BuildConfig.DEBUG) ({ settings = true }) else null)
        }
        is AuthState.Loading -> AuthPage {
            Mascot(Modifier.size(88.dp))
            CircularProgressIndicator()
            Text(state.label)
        }
        is AuthState.Retry -> AuthPage {
            Mascot(Modifier.size(88.dp))
            Text("Chưa kiểm tra được phiên đăng nhập", style = MaterialTheme.typography.titleLarge)
            Text(state.message)
            PlayButton("Thử lại", onClick = retry)
            TextButton(onClick = logout) { Text("Đăng nhập tài khoản khác") }
            if(BuildConfig.DEBUG) TextButton(onClick = { settings = true }) { Text("Cài đặt kết nối") }
        }
    }
    if (settings && state !is AuthState.SignedIn) ServerSettings(server,
        onDismiss = { settings = false }, onSave = { changeServer(it); settings = false })
}

@Composable
private fun AuthPage(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Background).safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally,
            content = content)
    }
}

@Composable
fun LoginScreen(state: AuthState.Login, onSignIn: (String, String) -> Unit, onSettings: (() -> Unit)? = null) {
    var username by rememberSaveable { mutableStateOf("") }
    // Password and JWT never enter rememberSaveable, savedState or preference fields.
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    fun submit() {
        attempted = true
        if(username.isNotBlank() && password.isNotEmpty() && !state.submitting) onSignIn(username.trim(), password)
    }
    AuthPage {
        Mascot(Modifier.size(96.dp))
        Text("Vui Học Lớp 1", style = MaterialTheme.typography.headlineLarge, color = SkyDark)
        Text("Cùng bé khám phá chữ cái", color = Muted)
        Paper {
            Text("Đăng nhập", style = MaterialTheme.typography.headlineMedium)
            Text("Dùng tài khoản học sinh hoặc phụ huynh", color = Muted)
            OutlinedTextField(username, onValueChange = { username = it },
                label = { Text("Tên đăng nhập hoặc email") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("login-username"), enabled = !state.submitting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = attempted && username.isBlank(), supportingText = {
                    if(attempted && username.isBlank()) Text("Nhập tên đăng nhập hoặc email")
                })
            OutlinedTextField(password, onValueChange = { password = it }, label = { Text("Mật khẩu") },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("login-password"), enabled = !state.submitting,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }), isError = attempted && password.isEmpty(),
                supportingText = { if(attempted && password.isEmpty()) Text("Nhập mật khẩu") },
                trailingIcon = { IconButton(onClick = { visible = !visible }, enabled = !state.submitting) {
                    Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        if (visible) "Ẩn mật khẩu" else "Hiện mật khẩu")
                } })
            if (state.message != null) Text(state.message, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("login-error"))
            if (state.submitting) Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Text("Đang đăng nhập…")
            }
            PlayButton("Đăng nhập", icon = Icons.AutoMirrored.Rounded.Login, enabled = !state.submitting,
                modifier = Modifier.testTag("login-submit"), onClick = { submit() })
        }
        if(onSettings != null) TextButton(onClick = onSettings, enabled = !state.submitting) { Text("Cài đặt kết nối") }
    }
}

@Composable
private fun ServerSettings(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf(current) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Kết nối backend local") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Điện thoại: dùng IP máy tính cùng mạng. Máy ảo Android: dùng 10.0.2.2.")
                OutlinedTextField(value, onValueChange = { value = it; error = null },
                    label = { Text("Địa chỉ máy chủ") }, placeholder = { Text("http://IP-máy-tính:8080/") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                if(error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        }, confirmButton = { TextButton(onClick = {
            try { onSave(normalizeServer(value, true)) } catch(e: AuthFailure) { error = e.publicMessage }
        }) { Text("Lưu kết nối") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}
