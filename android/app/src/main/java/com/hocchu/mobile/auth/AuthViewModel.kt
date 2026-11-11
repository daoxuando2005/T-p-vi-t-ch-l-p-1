package com.hocchu.mobile.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hocchu.mobile.BuildConfig
import kotlinx.coroutines.*

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = app.getSharedPreferences("backend.connection", 0)
    private val repository = AuthRepository(AuthApi(), SessionVault(app.getSharedPreferences("auth.session", 0)), BuildConfig.DEBUG)
    var state: AuthState by mutableStateOf(AuthState.Loading("Đang kiểm tra phiên đăng nhập…"))
        private set
    var server: String by mutableStateOf(if(BuildConfig.DEBUG)
        settings.getString("server", BuildConfig.DEFAULT_API_BASE_URL) ?: BuildConfig.DEFAULT_API_BASE_URL
        else BuildConfig.DEFAULT_API_BASE_URL)
        private set
    private var request: Job? = null
    private var expiry: Job? = null
    init { restore() }

    fun restore() {
        request?.cancel()
        request = viewModelScope.launch {
            state = AuthState.Loading("Đang kiểm tra phiên đăng nhập…")
            try {
                val session = repository.restore()
                if (session == null) state = AuthState.Login() else signedIn(session)
            } catch(e: CancellationException) { throw e }
            catch(e: AuthFailure) {
                if (e is AuthFailure.Expired || e is AuthFailure.MobileOnly || e is AuthFailure.Configuration) {
                    try { repository.clear(); state = AuthState.Login(e.publicMessage) }
                    catch(cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { state = AuthState.Retry(AuthFailure.Storage().publicMessage) }
                } else state = AuthState.Retry(e.publicMessage)
            } catch (_: Exception) { state = AuthState.Retry(AuthFailure.Storage().publicMessage) }
        }
    }
    fun signIn(username: String, password: String) {
        if ((state as? AuthState.Login)?.submitting == true) return
        request?.cancel()
        request = viewModelScope.launch {
            state = AuthState.Login(submitting = true)
            try { signedIn(repository.signIn(server, username, password)) }
            catch (e: CancellationException) { throw e }
            catch (e: AuthFailure) { state = AuthState.Login(e.publicMessage) }
            catch (_: Exception) { state = AuthState.Login(AuthFailure.Server().publicMessage) }
        }
    }
    private fun signedIn(session: AuthSession) {
        expiry?.cancel()
        server = session.server
        state = AuthState.SignedIn(session)
        expiry = viewModelScope.launch {
            delay((session.expiresAt - System.currentTimeMillis()).coerceAtLeast(0))
            logout(AuthFailure.Expired().publicMessage)
        }
    }
    fun logout(message: String? = null) {
        request?.cancel(); expiry?.cancel()
        request = viewModelScope.launch {
            state = AuthState.Loading("Đang đăng xuất…")
            try { repository.clear(); state = AuthState.Login(message) }
            catch(cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state = AuthState.Retry(AuthFailure.Storage().publicMessage) }
        }
    }
    fun changeServer(value: String) {
        if (!BuildConfig.DEBUG) return
        val normalized = normalizeServer(value, allowHttp = true)
        request?.cancel(); expiry?.cancel()
        request = viewModelScope.launch {
            state = AuthState.Loading("Đang cập nhật kết nối…")
            try {
                repository.clear()
                withContext(Dispatchers.IO) {
                    if (!settings.edit().putString("server", normalized).commit()) throw AuthFailure.Storage()
                }
                server = normalized
                state = AuthState.Login()
            } catch(cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state = AuthState.Retry(AuthFailure.Storage().publicMessage) }
        }
    }
}
