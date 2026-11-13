package com.hocchu.mobile.auth

import android.util.Base64
import org.json.JSONObject
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class HttpReply(val status: Int, val body: String) {
    override fun toString() = "HttpReply(status=$status, body=[redacted])"
}
fun interface AuthTransport {
    fun request(url: String, method: String, token: String?, body: String?): HttpReply
}

class UrlAuthTransport(private val maxResponseBytes: Int = 65536) : AuthTransport {
    override fun request(url: String, method: String, token: String?, body: String?): HttpReply {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.instanceFollowRedirects = false // Never forward credentials to another host.
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if(status in 200..299) connection.inputStream else connection.errorStream
            // Bound replies; authentication keeps the default 64 KiB limit.
            val bytes = stream?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (output.size() <= maxResponseBytes) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: byteArrayOf()
            if (bytes.size > maxResponseBytes) throw AuthFailure.InvalidResponse()
            return HttpReply(status, bytes.toString(Charsets.UTF_8))
        } catch (e: IOException) {
            throw AuthFailure.Network()
        } finally { connection.disconnect() }
    }
}

class AuthApi(private val transport: AuthTransport = UrlAuthTransport()) {
    fun register(server: String, input: RegistrationInput) {
        if(input.errors().isNotEmpty()) throw AuthFailure.Registration()
        val payload = JSONObject().put("fullName", input.fullName.trim()).put("username", input.username.trim())
            .put("email", input.email.trim()).put("password", input.password).put("role", input.role)
        val reply = transport.request(server + "api/auth/signup", "POST", null, payload.toString())
        when {
            reply.status == 409 -> throw AuthFailure.Registration(conflict = true)
            reply.status == 400 -> {
                // Only map known public conflicts; never display an arbitrary server body.
                val message = runCatching { json(reply.body).optString("message") }.getOrDefault("")
                throw AuthFailure.Registration(conflict = message in setOf("Tên đăng nhập đã tồn tại!", "Email đã được sử dụng!"))
            }
            reply.status !in 200..299 -> throw AuthFailure.Server()
        }
        if(json(reply.body).optBoolean("success", false) != true) throw AuthFailure.Registration()
    }
    fun signIn(server: String, username: String, password: String): AuthSession {
        val payload = JSONObject().put("usernameOrEmail", username.trim()).put("password", password)
        val reply = transport.request(server + "api/auth/signin", "POST", null, payload.toString())
        checkStatus(reply.status, signingIn = true)
        return parseSession(server, reply.body)
    }
    fun me(session: AuthSession): AuthSession {
        if (session.expiresAt <= System.currentTimeMillis()) throw AuthFailure.Expired()
        val reply = transport.request(session.server + "api/auth/me", "GET", session.token, null)
        checkStatus(reply.status, signingIn = false)
        val account = parseAccount(json(reply.body))
        if (account.id != session.account.id) throw AuthFailure.InvalidResponse()
        if (session.expiresAt <= System.currentTimeMillis()) throw AuthFailure.Expired()
        return session.copy(account = account)
    }
    private fun checkStatus(status: Int, signingIn: Boolean) {
        when {
            status in 200..299 -> Unit
            status == 401 || status == 403 -> if(signingIn) throw AuthFailure.Credentials() else throw AuthFailure.Expired()
            status == 400 && signingIn -> throw AuthFailure.Credentials()
            else -> throw AuthFailure.Server()
        }
    }
    companion object {
        fun json(value: String): JSONObject = try { JSONObject(value) } catch (_: Exception) { throw AuthFailure.InvalidResponse() }
        fun parseAccount(value: JSONObject): UserAccount {
            try {
                val id = value.getLong("id")
                val username = value.getString("username")
                val rolesJson = value.getJSONArray("roles")
                val roles = (0 until rolesJson.length()).map { rolesJson.getString(it) }.toSet()
                if (id <= 0 || username.isBlank()) throw AuthFailure.InvalidResponse()
                if ("ROLE_STUDENT" !in roles && "ROLE_PARENT" !in roles) throw AuthFailure.MobileOnly()
                return UserAccount(id, username, value.optString("fullName").takeUnless { it.isBlank() || it == "null" } ?: username, roles)
            } catch (e: AuthFailure) { throw e } catch (_: Exception) { throw AuthFailure.InvalidResponse() }
        }
        fun parseSession(server: String, value: String): AuthSession {
            val json = json(value)
            val account = parseAccount(json)
            val token = json.optString("accessToken")
            if (!token.matches(Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")) ||
                json.optString("tokenType") != "Bearer") throw AuthFailure.InvalidResponse()
            val expiresAt = try {
                val payload = Base64.decode(token.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP)
                Math.multiplyExact(JSONObject(payload.toString(Charsets.UTF_8)).getLong("exp"), 1000L)
            } catch (_: Exception) { throw AuthFailure.InvalidResponse() }
            if (expiresAt <= System.currentTimeMillis()) throw AuthFailure.Expired()
            // exp is used for local timeout only; /me verifies the actual JWT on the server.
            return AuthSession(server, token, account, expiresAt)
        }
    }
}
