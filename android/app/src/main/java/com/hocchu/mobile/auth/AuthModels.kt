package com.hocchu.mobile.auth

import java.net.URI
import java.security.MessageDigest

data class UserAccount(val id: Long, val username: String, val fullName: String, val roles: Set<String>)
data class AuthSession(val server: String, val token: String, val account: UserAccount, val expiresAt: Long) {
    override fun toString() = "AuthSession(userId=${account.id}, token=[redacted])"
}

/** Server identity + immutable user id, never display name, identifies local learning data. */
fun accountPreferences(server: String, id: Long): String {
    require(id > 0)
    val digest = MessageDigest.getInstance("SHA-256").digest(normalizeServer(server, true).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    return "learning.user.$digest.$id"
}

fun normalizeServer(value: String, allowHttp: Boolean): String {
    val uri = try { URI(value.trim()) } catch (_: Exception) { throw AuthFailure.Configuration() }
    val scheme = uri.scheme?.lowercase()
    if (scheme !in (if (allowHttp) listOf("http", "https") else listOf("https")) ||
        uri.host.isNullOrBlank() || uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null ||
        uri.port !in -1..65535 || uri.port == 0 || uri.path !in listOf("", "/", "/api", "/api/")) throw AuthFailure.Configuration()
    val port = if ((scheme == "http" && uri.port == 80) || (scheme == "https" && uri.port == 443)) -1 else uri.port
    return URI(scheme, null, uri.host.lowercase(), port, "/", null, null).toASCIIString()
}

sealed class AuthFailure(val publicMessage: String) : Exception(publicMessage) {
    class Configuration : AuthFailure("Địa chỉ máy chủ chưa đúng. Vui lòng kiểm tra kết nối.")
    class Credentials : AuthFailure("Tên đăng nhập hoặc mật khẩu chưa đúng, hoặc tài khoản chưa được kích hoạt.")
    class Expired : AuthFailure("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
    class MobileOnly : AuthFailure("Vui lòng dùng tài khoản học sinh hoặc phụ huynh để đăng nhập app.")
    class Network : AuthFailure("Chưa kết nối được máy chủ. Hãy kiểm tra mạng và thử lại.")
    class Server : AuthFailure("Máy chủ chưa xử lý được yêu cầu. Vui lòng thử lại sau.")
    class InvalidResponse : AuthFailure("Dữ liệu đăng nhập từ máy chủ chưa hợp lệ.")
    class Storage : AuthFailure("Chưa lưu được phiên đăng nhập trên máy. Vui lòng thử lại.")
    class Registration(val conflict: Boolean = false) : AuthFailure(if(conflict)
        "Tên đăng nhập hoặc email đã được sử dụng. Hãy chọn thông tin khác."
        else "Chưa tạo được tài khoản. Kiểm tra thông tin hoặc thử lại sau.")
}

data class RegistrationInput(val fullName: String, val username: String, val email: String, val password: String, val role: String) {
    override fun toString() = "RegistrationInput(role=$role, credentials=[redacted])"
    fun errors(): Map<String,String> = buildMap {
        if(fullName.trim().isEmpty() || fullName.trim().length > 100) put("fullName", "Nhập họ tên, tối đa 100 ký tự")
        if(!username.trim().matches(Regex("[A-Za-z0-9_.-]{3,50}"))) put("username", "Dùng 3–50 chữ không dấu, số hoặc dấu . _ -")
        if(email.trim().length > 100 || !email.trim().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) put("email", "Nhập địa chỉ email hợp lệ, tối đa 100 ký tự")
        if(password.length < 6 || password.toByteArray(Charsets.UTF_8).size > 72) put("password", "Mật khẩu từ 6 ký tự, tối đa 72 byte")
        if(role !in setOf("STUDENT", "PARENT")) put("role", "Chọn học sinh hoặc phụ huynh")
    }
}

sealed interface AuthState {
    data class Login(val message: String? = null, val submitting: Boolean = false,
                     val successMessage: String? = null, val initialUsername: String = "") : AuthState
    data class Register(val message: String? = null, val submitting: Boolean = false) : AuthState
    data class Loading(val label: String) : AuthState
    data class SignedIn(val session: AuthSession) : AuthState
    data class Retry(val message: String) : AuthState
}
