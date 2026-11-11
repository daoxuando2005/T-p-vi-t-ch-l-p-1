package com.hocchu.mobile

import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.hocchu.mobile.auth.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AuthProtocolTest {
    private val server = "https://school.example/"
    private fun response(id: Long = 7, role: String = "ROLE_STUDENT", expires: Long = System.currentTimeMillis() / 1000 + 3600): String {
        fun b64(value: String) = Base64.encodeToString(value.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val token = b64("{}") + "." + b64(JSONObject().put("exp", expires).toString()) + ".signature"
        return JSONObject().put("accessToken", token).put("tokenType", "Bearer").put("id", id)
            .put("username", "test-student").put("fullName", "Học sinh thử nghiệm")
            .put("roles", JSONArray(listOf(role))).toString()
    }
    private class MemoryStorage : SessionStorage {
        @Volatile var value: AuthSession? = null
        override fun load() = value
        override fun save(session: AuthSession) { value = session }
        override fun clear() { value = null }
    }
    @Test fun signInSendsExactFieldsAndMeKeepsTheOriginalToken() {
        val reply = response()
        val parsed = AuthApi.parseSession(server, reply)
        val api = AuthApi(AuthTransport { url, method, token, body ->
            if (url.endsWith("signin")) {
                assertEquals("POST", method); assertNull(token)
                val json = JSONObject(body!!)
                assertEquals("student", json.getString("usernameOrEmail"))
                assertEquals(" test password ", json.getString("password"))
                HttpReply(200, reply)
            } else {
                assertEquals(server + "api/auth/me", url); assertEquals("GET", method)
                assertEquals(parsed.token, token); assertNull(body)
                HttpReply(200, JSONObject(reply).put("accessToken", JSONObject.NULL).toString())
            }
        })
        val session = api.signIn(server, " student ", " test password ")
        assertEquals(session.token, api.me(session).token)
        assertFalse(session.toString().contains(session.token))
    }
    @Test fun roleExpiredJwtAndMalformedResponsesCannotOpenStudentUi() {
        assertTrue(AuthApi.parseSession(server, response(role = "ROLE_PARENT")).account.roles.contains("ROLE_PARENT"))
        try { AuthApi.parseSession(server, response(role = "ROLE_ADMIN")); fail() } catch (_: AuthFailure.MobileOnly) { }
        try { AuthApi.parseSession(server, response(role = "ROLE_TEACHER")); fail() } catch (_: AuthFailure.MobileOnly) { }
        try { AuthApi.parseSession(server, response(expires = 1)); fail() } catch (_: AuthFailure.Expired) { }
        try { AuthApi.parseSession(server, "<html>error</html>"); fail() } catch (_: AuthFailure.InvalidResponse) { }
        val valid = AuthApi.parseSession(server, response())
        val mismatch = AuthApi(AuthTransport { _, _, _, _ -> HttpReply(200, response(id = 8)) })
        try { mismatch.me(valid); fail() } catch (_: AuthFailure.InvalidResponse) { }
    }
    @Test fun unauthorizedLoginAndExpiredRestorationHaveDifferentMessages() {
        val api = AuthApi(AuthTransport { _, _, _, _ -> HttpReply(401, "do not expose this server body") })
        try { api.signIn(server, "student", "wrong"); fail() } catch (_: AuthFailure.Credentials) { }
        try { api.me(AuthApi.parseSession(server, response())); fail() } catch (_: AuthFailure.Expired) { }
    }
    @Test fun verifiedSessionPersistsAndLogoutDoesNotEraseAccountProgress() = runBlocking {
        val storage = MemoryStorage()
        val api = AuthApi(AuthTransport { _, _, _, _ -> HttpReply(200, response()) })
        val repository = AuthRepository(api, storage, false)
        val session = repository.signIn(server, "student", "test password")
        assertEquals(session, repository.restore())
        assertEquals(session, storage.value)
        repository.clear()
        assertNull(repository.restore())
        assertEquals(accountPreferences(server, session.account.id), accountPreferences(server, 7))
    }
    @Test fun failedMeNeverPersistsAnUnverifiedSignIn() = runBlocking {
        val storage = MemoryStorage()
        val api = AuthApi(AuthTransport { url, _, _, _ -> if(url.endsWith("signin")) HttpReply(200, response()) else HttpReply(401, "") })
        try { AuthRepository(api, storage, false).signIn(server, "student", "test password"); fail() }
        catch (_: AuthFailure.Expired) { }
        assertNull(storage.value)
    }
    @Test fun logoutWinsOverACancelledNetworkLogin() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val storage = MemoryStorage()
        val api = AuthApi(AuthTransport { _, _, _, _ ->
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            HttpReply(200, response())
        })
        val repository = AuthRepository(api, storage, false)
        val request = launch { repository.signIn(server, "student", "test password") }
        withContext(Dispatchers.IO) { check(entered.await(5, TimeUnit.SECONDS)) }
        request.cancel()
        repository.clear()
        release.countDown()
        request.join()
        assertNull(storage.value)
    }
    @Test fun vaultEncryptsRestoresAndClearsTamperedData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "auth-test-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        val alias = name
        try {
            val vault = SessionVault(prefs, alias)
            val session = AuthApi.parseSession(server, response())
            vault.save(session)
            val saved = prefs.getString("sealed.v1", "")!!
            assertFalse(saved.contains(session.token)); assertFalse(saved.contains(session.account.username))
            assertEquals(session, vault.load())
            prefs.edit().putString("sealed.v1", saved.dropLast(5) + "AAAAA").commit()
            assertNull(vault.load())
            assertFalse(prefs.contains("sealed.v1"))
            vault.save(session); vault.clear(); assertNull(vault.load())
            prefs.edit().putInt("sealed.v1", 123).commit()
            assertNull(vault.load())
        } finally {
            context.deleteSharedPreferences(name)
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        }
    }
}
