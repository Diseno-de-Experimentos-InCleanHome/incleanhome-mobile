package com.incleanhome.mobile.core.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class UserSession(
    val userId: Int,
    val role: String,
    val name: String,
    val email: String,
    val token: String
)

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class Authenticated(val session: UserSession) : SessionState
}

class SessionManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    @Volatile
    private var currentSession: UserSession? = null

    fun currentToken(): String? = currentSession?.token

    suspend fun restoreSession() = withContext(Dispatchers.IO) {
        val restoredSession = runCatching {
            preferences.getString(ENCRYPTED_SESSION_KEY, null)
                ?.let(::decrypt)
                ?.let { gson.fromJson(it, UserSession::class.java) }
                ?.takeIf(::isValidSession)
        }.getOrNull()

        if (restoredSession == null) {
            preferences.edit().remove(ENCRYPTED_SESSION_KEY).commit()
            currentSession = null
            _sessionState.value = SessionState.LoggedOut
        } else {
            currentSession = restoredSession
            _sessionState.value = SessionState.Authenticated(restoredSession)
        }
    }

    suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        require(isValidSession(session)) { "Invalid session data" }
        val encryptedSession = encrypt(gson.toJson(session))
        check(
            preferences.edit()
                .putString(ENCRYPTED_SESSION_KEY, encryptedSession)
                .commit()
        ) { "Could not persist session" }

        currentSession = session
        _sessionState.value = SessionState.Authenticated(session)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        check(
            preferences.edit()
                .remove(ENCRYPTED_SESSION_KEY)
                .commit()
        ) { "Could not clear session" }

        currentSession = null
        _sessionState.value = SessionState.LoggedOut
    }

    private fun isValidSession(session: UserSession): Boolean {
        val knownRole = session.role.equals(CLIENT_ROLE, ignoreCase = true) ||
            session.role.equals(WORKER_ROLE, ignoreCase = true)
        return session.userId > 0 && session.token.isNotBlank() && knownRole
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return gson.toJson(
            EncryptedPayload(
                iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
                ciphertext = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            )
        )
    }

    private fun decrypt(serializedPayload: String): String {
        val payload = gson.fromJson(serializedPayload, EncryptedPayload::class.java)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(payload.iv, Base64.NO_WRAP))
        )
        val plainBytes = cipher.doFinal(Base64.decode(payload.ciphertext, Base64.NO_WRAP))
        return plainBytes.toString(Charsets.UTF_8)
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    private data class EncryptedPayload(
        val iv: String,
        val ciphertext: String
    )

    companion object {
        const val CLIENT_ROLE = "client"
        const val WORKER_ROLE = "worker"

        private const val PREFERENCES_NAME = "incleanhome_secure_session"
        private const val ENCRYPTED_SESSION_KEY = "encrypted_session"
        private const val KEY_ALIAS = "incleanhome_session_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
