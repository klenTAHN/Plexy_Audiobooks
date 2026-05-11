package com.klentahn.plexyaudiobooks.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    private val AUTH_TOKEN = stringPreferencesKey("auth_token")
    private val SERVER_URI = stringPreferencesKey("server_uri")
    private val SERVER_ID = stringPreferencesKey("server_id")
    private val LIBRARY_KEY = stringPreferencesKey("library_key")
    private val LIBRARY_TITLE = stringPreferencesKey("library_title")
    private val CLIENT_IDENTIFIER = stringPreferencesKey("client_identifier")
    private val IS_MANUAL_TOKEN = booleanPreferencesKey("is_manual_token")

    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val serverUri: Flow<String?> = context.dataStore.data.map { it[SERVER_URI] }
    val serverId: Flow<String?> = context.dataStore.data.map { it[SERVER_ID] }
    val libraryKey: Flow<String?> = context.dataStore.data.map { it[LIBRARY_KEY] }
    val libraryTitle: Flow<String?> = context.dataStore.data.map { it[LIBRARY_TITLE] }
    val clientIdentifier: Flow<String?> = context.dataStore.data.map { it[CLIENT_IDENTIFIER] }
    val isManualToken: Flow<Boolean> = context.dataStore.data.map { it[IS_MANUAL_TOKEN] ?: false }

    suspend fun saveAuthToken(token: String, isManual: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
            prefs[IS_MANUAL_TOKEN] = isManual
        }
    }

    suspend fun saveServerUri(uri: String) {
        context.dataStore.edit { it[SERVER_URI] = uri }
    }

    suspend fun saveServer(id: String, uri: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_ID] = id
            prefs[SERVER_URI] = uri
        }
    }

    suspend fun saveLibraryKey(key: String) {
        context.dataStore.edit { it[LIBRARY_KEY] = key }
    }

    suspend fun saveLibrary(key: String, title: String) {
        context.dataStore.edit { prefs ->
            prefs[LIBRARY_KEY] = key
            prefs[LIBRARY_TITLE] = title
        }
    }

    suspend fun saveClientIdentifier(id: String) {
        context.dataStore.edit { it[CLIENT_IDENTIFIER] = id }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN)
            prefs.remove(IS_MANUAL_TOKEN)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
