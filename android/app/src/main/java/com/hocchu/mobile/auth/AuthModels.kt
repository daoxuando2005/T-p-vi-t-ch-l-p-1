package com.hocchu.mobile.auth

import java.net.URI
import java.security.MessageDigest

data class StudentAccount(val id: Long, val username: String, val fullName: String, val roles: Set<String>)
data class AuthSession(val server: String, val token: String, val account: StudentAccount, val expiresAt: Long) {
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
    class StudentOnly : AuthFailure("Ứng dụng này dành cho học sinh. Vui lòng dùng tài khoản học sinh.")
    class Network : AuthFailure("Chưa kết nối được máy chủ. Hãy kiểm tra mạng và thử lại.")
    class Server : AuthFailure("Máy chủ chưa xử lý được yêu cầu. Vui lòng thử lại sau.")
    class InvalidResponse : AuthFailure("Dữ liệu đăng nhập từ máy chủ chưa hợp lệ.")
    class Storage : AuthFailure("Chưa lưu được phiên đăng nhập trên máy. Vui lòng thử lại.")
}

sealed interface AuthState {
    data class Login(val message: String? = null, val submitting: Boolean = false) : AuthState
    data class Loading(val label: String) : AuthState
    data class SignedIn(val session: AuthSession) : AuthState
    data class Retry(val message: String) : AuthState
}
