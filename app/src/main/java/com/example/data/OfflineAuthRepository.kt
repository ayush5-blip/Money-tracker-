package com.example.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.AppDatabase
import com.example.data.local.LocalUserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.sessionDataStore by preferencesDataStore(name = "ledger_offline_session")

data class OfflineSessionUser(
    val userId: String,
    val username: String,
    val displayName: String
)

class OfflineAuthRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val userDao = db.localUserDao()
    private val activeUserKey = stringPreferencesKey("active_user_id")
    private val previewSeededKey = booleanPreferencesKey("preview_account_seeded")

    companion object {
        const val PREVIEW_USERNAME = "demo@ledger.local"
        const val PREVIEW_PASSWORD = "1234"
        private val seedMutex = Mutex()
    }

    val currentUserFlow: Flow<OfflineSessionUser?> = context.sessionDataStore.data.map { prefs ->
        val uid = prefs[activeUserKey] ?: return@map null
        val user = userDao.findById(uid) ?: return@map null
        OfflineSessionUser(
            userId = user.userId,
            username = user.username,
            displayName = user.displayName
        )
    }

    val existingAccountsFlow: Flow<List<OfflineSessionUser>> = userDao.observeAllUsers().map { list ->
        list.map {
            OfflineSessionUser(
                userId = it.userId,
                username = it.username,
                displayName = it.displayName
            )
        }
    }

    suspend fun ensurePreviewAccountSeeded() = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            val prefs = context.sessionDataStore.data.first()
            if (prefs[previewSeededKey] == true) return@withLock

            if (userDao.findByUsername(PREVIEW_USERNAME) == null) {
                val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val hash = hashPassword(PREVIEW_PASSWORD, salt)
                val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
                userDao.insertUser(
                    LocalUserEntity(
                        userId = "usr_preview_demo",
                        username = PREVIEW_USERNAME,
                        usernameLower = PREVIEW_USERNAME,
                        displayName = "Preview Tester",
                        passwordHash = hash,
                        saltBase64 = saltB64,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
            }
            context.sessionDataStore.edit { it[previewSeededKey] = true }
        }
    }

    suspend fun register(
        usernameRaw: String,
        displayNameRaw: String,
        password: String
    ): Result<OfflineSessionUser> = withContext(Dispatchers.IO) {
        try {
            val username = usernameRaw.trim()
            val usernameLower = username.lowercase(Locale.ROOT)
            val displayName = displayNameRaw.trim().ifEmpty { username }

            require(username.length >= 3) { "Username must be at least 3 characters." }
            require(password.length >= 4) { "Password must be at least 4 characters." }

            if (userDao.findByUsername(usernameLower) != null) {
                return@withContext Result.failure(IllegalArgumentException("Account '$username' already exists. Please sign in."))
            }

            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val hash = hashPassword(password, salt)
            val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)

            val userId = "usr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val entity = LocalUserEntity(
                userId = userId,
                username = username,
                usernameLower = usernameLower,
                displayName = displayName,
                passwordHash = hash,
                saltBase64 = saltB64,
                createdAtMillis = System.currentTimeMillis()
            )
            userDao.insertUser(entity)
            context.sessionDataStore.edit { it[activeUserKey] = userId }

            Result.success(OfflineSessionUser(userId, username, displayName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(usernameRaw: String, password: String): Result<OfflineSessionUser> =
        withContext(Dispatchers.IO) {
            try {
                val usernameLower = usernameRaw.trim().lowercase(Locale.ROOT)
                require(usernameLower.isNotEmpty()) { "Enter your username." }
                require(password.isNotEmpty()) { "Enter your password." }

                val user = userDao.findByUsername(usernameLower)
                    ?: return@withContext Result.failure(IllegalArgumentException("No offline account found for '$usernameRaw'."))

                val salt = Base64.decode(user.saltBase64, Base64.NO_WRAP)
                val computed = hashPassword(password, salt)
                if (!constantTimeEquals(computed, user.passwordHash)) {
                    return@withContext Result.failure(IllegalArgumentException("Incorrect password."))
                }

                context.sessionDataStore.edit { it[activeUserKey] = user.userId }
                Result.success(OfflineSessionUser(user.userId, user.username, user.displayName))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun logout() {
        context.sessionDataStore.edit { it.remove(activeUserKey) }
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 12_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }
}
