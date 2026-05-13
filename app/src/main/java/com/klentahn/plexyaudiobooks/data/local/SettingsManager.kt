package com.klentahn.plexyaudiobooks.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _authToken = MutableStateFlow(sharedPreferences.getString(PLEX_AUTH_TOKEN_KEY, null))
    val authToken: Flow<String?> = _authToken.asStateFlow()

    companion object {
        private const val PLEX_AUTH_TOKEN_KEY = "plex_auth_token"
        private const val DB_PASSPHRASE_KEY = "db_passphrase"
        private val PLEX_SERVER_ID = stringPreferencesKey("plex_server_id")
        private val PLEX_SERVER_URI = stringPreferencesKey("plex_server_uri")
        private val PLEX_LIBRARY_KEY = stringPreferencesKey("plex_library_key")
        private val PLEX_LIBRARY_TITLE = stringPreferencesKey("plex_library_title")
        private val PLEX_CLIENT_IDENTIFIER = stringPreferencesKey("client_identifier")
        private val IS_MANUAL_TOKEN = booleanPreferencesKey("is_manual_token")
    }

    val serverId: Flow<String?> = context.dataStore.data.map { it[PLEX_SERVER_ID] }
    val serverUri: Flow<String?> = context.dataStore.data.map { it[PLEX_SERVER_URI] }
    val libraryKey: Flow<String?> = context.dataStore.data.map { it[PLEX_LIBRARY_KEY] }
    val libraryTitle: Flow<String?> = context.dataStore.data.map { it[PLEX_LIBRARY_TITLE] }
    val clientIdentifier: Flow<String?> = context.dataStore.data.map { it[PLEX_CLIENT_IDENTIFIER] }
    val isManualToken: Flow<Boolean> = context.dataStore.data.map { it[IS_MANUAL_TOKEN] ?: false }

    suspend fun saveAuthToken(token: String, isManual: Boolean = false) {
        sharedPreferences.edit().putString(PLEX_AUTH_TOKEN_KEY, token).apply()
        _authToken.value = token
        context.dataStore.edit { it[IS_MANUAL_TOKEN] = isManual }
    }

    suspend fun saveServerUri(uri: String) {
        context.dataStore.edit { it[PLEX_SERVER_URI] = uri }
    }

    suspend fun saveServer(id: String, uri: String) {
        context.dataStore.edit { prefs ->
            prefs[PLEX_SERVER_ID] = id
            prefs[PLEX_SERVER_URI] = uri
        }
    }

    suspend fun saveLibraryKey(key: String) {
        context.dataStore.edit { it[PLEX_LIBRARY_KEY] = key }
    }

    suspend fun saveLibrary(key: String, title: String) {
        context.dataStore.edit { prefs ->
            prefs[PLEX_LIBRARY_KEY] = key
            prefs[PLEX_LIBRARY_TITLE] = title
        }
    }

    suspend fun saveClientIdentifier(id: String) {
        context.dataStore.edit { it[PLEX_CLIENT_IDENTIFIER] = id }
    }

    suspend fun clearAuth() {
        sharedPreferences.edit().remove(PLEX_AUTH_TOKEN_KEY).apply()
        _authToken.value = null
        context.dataStore.edit { it.remove(IS_MANUAL_TOKEN) }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
        sharedPreferences.edit().clear().apply()
        _authToken.value = null
    }

    suspend fun clearAll() {
        clear()
    }

    fun getDatabasePassphrase(): ByteArray {
        val existing = sharedPreferences.getString(DB_PASSPHRASE_KEY, null)
        return if (existing != null) {
            android.util.Base64.decode(existing, android.util.Base64.DEFAULT)
        } else {
            val newPassphrase = ByteArray(32)
            java.security.SecureRandom().nextBytes(newPassphrase)
            val encoded = android.util.Base64.encodeToString(newPassphrase, android.util.Base64.DEFAULT)
            sharedPreferences.edit().putString(DB_PASSPHRASE_KEY, encoded).apply()
            newPassphrase
        }
    }
}
