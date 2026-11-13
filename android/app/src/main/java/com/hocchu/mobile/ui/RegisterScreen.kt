package com.hocchu.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.hocchu.mobile.auth.*

@Composable
internal fun RegisterScreen(state: AuthState.Register, onRegister: (RegistrationInput) -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("STUDENT") }
    // Credentials must not enter Bundle/SavedState or preferences.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    val input = RegistrationInput(name, username, email, password, role)
    val errors = if(attempted) input.errors() else emptyMap()
    BackHandler { if(!state.submitting) onBack() }
    AuthPage {
        Mascot(Modifier.size(72.dp))
        Text("Bắt đầu cùng Vui Học", style = MaterialTheme.typography.headlineMedium, color = SkyDark)
        Paper {
            Text("Tạo tài khoản", style = MaterialTheme.typography.headlineMedium)
            Text("Bạn đăng ký cho ai?", color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("STUDENT" to "Học sinh", "PARENT" to "Phụ huynh").forEach { (value, label) ->
                    FilterChip(selected = role == value, onClick = { role = value }, enabled = !state.submitting,
                        label = { Text(label) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("register-role-$value"))
                }
            }
            RegistrationField(name, { name = it }, "Họ và tên", "fullName", errors["fullName"], !state.submitting)
            RegistrationField(username, { username = it }, "Tên đăng nhập", "username", errors["username"], !state.submitting)
            RegistrationField(email, { email = it }, "Email", "email", errors["email"], !state.submitting, KeyboardType.Email)
            OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register-password"), enabled = !state.submitting,
                isError = errors["password"] != null, supportingText = { Text(errors["password"] ?: "Từ 6 ký tự, giữ riêng mật khẩu của bạn") },
                visualTransformation = if(visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                trailingIcon = { IconButton(onClick = { visible = !visible }, enabled = !state.submitting) {
                    Icon(if(visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if(visible) "Ẩn mật khẩu" else "Hiện mật khẩu")
                } })
            val mismatch = attempted && confirmation != password
            OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Nhập lại mật khẩu") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register-confirmation"), enabled = !state.submitting,
                visualTransformation = PasswordVisualTransformation(), isError = mismatch,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                supportingText = { if(mismatch) Text("Hai mật khẩu chưa khớp") })
            if(state.message != null) Text(state.message, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("register-error").semantics { liveRegion = LiveRegionMode.Polite })
            if(state.submitting) Text("Đang tạo tài khoản…", color = SkyDark)
            PlayButton(if(state.submitting) "Đang đăng ký…" else "Tạo tài khoản", enabled = !state.submitting,
                modifier = Modifier.testTag("register-submit"), onClick = {
                    attempted = true
                    if(input.errors().isEmpty() && confirmation == password) onRegister(input)
                })
            TextButton(onClick = onBack, enabled = !state.submitting, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Đã có tài khoản? Đăng nhập")
            }
        }
    }
}

@Composable
private fun RegistrationField(value: String, change: (String) -> Unit, label: String, tag: String,
                              error: String?, enabled: Boolean, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = true, enabled = enabled,
        modifier = Modifier.fillMaxWidth().testTag("register-$tag"), isError = error != null,
        supportingText = { if(error != null) Text(error) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Next))
}
