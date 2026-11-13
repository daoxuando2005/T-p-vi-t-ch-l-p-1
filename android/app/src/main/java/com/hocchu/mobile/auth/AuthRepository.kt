package com.hocchu.mobile.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AuthRepository(private val api: AuthApi, private val storage: SessionStorage, private val allowHttp: Boolean) {
    private val writes = Mutex()
    suspend fun register(server: String, input: RegistrationInput) {
        val endpoint = normalizeServer(server, allowHttp)
        withContext(Dispatchers.IO) { api.register(endpoint, input) }
    }
    suspend fun restore(): AuthSession? {
        val cached = writes.withLock { withContext(Dispatchers.IO) { storage.load() } } ?: return null
        normalizeServer(cached.server, allowHttp)
        val verified = withContext(Dispatchers.IO) { api.me(cached) }
        save(verified)
        return verified
    }
    suspend fun signIn(server: String, username: String, password: String): AuthSession {
        val endpoint = normalizeServer(server, allowHttp)
        val candidate = withContext(Dispatchers.IO) { api.signIn(endpoint, username, password) }
        val verified = withContext(Dispatchers.IO) { api.me(candidate) }
        save(verified)
        return verified
    }
    private suspend fun save(value: AuthSession) = writes.withLock {
        withContext(Dispatchers.IO) { currentCoroutineContext().ensureActive(); storage.save(value) }
    }
    // Serializing mutations prevents a cancelled login from restoring a session after logout.
    suspend fun clear() = writes.withLock { withContext(Dispatchers.IO) { storage.clear() } }
}
