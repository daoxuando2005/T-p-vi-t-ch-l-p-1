package com.hocchu.mobile.auth

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SessionStorage {
    fun load(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

/** Android Keystore keeps the AES key outside SharedPreferences. No password is stored. */
class SessionVault(private val prefs: SharedPreferences, private val alias: String = "hocchu.auth.session.v1") : SessionStorage {
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }
    override fun save(session: AuthSession) {
        try {
            val account = session.account
            val plain = JSONObject().put("server", session.server).put("accessToken", session.token).put("tokenType", "Bearer")
                .put("id", account.id).put("username", account.username).put("fullName", account.fullName)
                .put("roles", JSONArray(account.roles.toList())).toString()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            if (!prefs.edit().putString("sealed.v1", encoded).commit()) throw AuthFailure.Storage()
        } catch (_: Exception) { throw AuthFailure.Storage() }
    }
    override fun load(): AuthSession? {
        try {
            val value = prefs.getString("sealed.v1", null) ?: return null
            val parts = value.split(':')
            require(parts.size == 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            val plain = cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
            val json = AuthApi.json(plain)
            return AuthApi.parseSession(json.getString("server"), plain)
        } catch (e: AuthFailure.Expired) { clear(); throw e }
        catch (_: Exception) { clear(); return null }
    }
    override fun clear() {
        if (!prefs.edit().remove("sealed.v1").commit()) throw AuthFailure.Storage()
    }
}
