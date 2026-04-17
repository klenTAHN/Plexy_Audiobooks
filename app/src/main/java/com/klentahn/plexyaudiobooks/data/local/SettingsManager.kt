package com.klentahn.plexyaudiobooks.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val PLEX_AUTH_TOKEN = stringPreferencesKey("plex_auth_token")
        val PLEX_SERVER_ID = stringPreferencesKey("plex_server_id")
        val PLEX_SERVER_URI = stringPreferencesKey("plex_server_uri")
        val PLEX_LIBRARY_KEY = stringPreferencesKey("plex_library_key")
        val PLEX_LIBRARY_TITLE = stringPreferencesKey("plex_library_title")
        val CLIENT_IDENTIFIER = stringPreferencesKey("client_identifier")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PLEX_AUTH_TOKEN]
    }

    val serverId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PLEX_SERVER_ID]
    }

    val serverUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PLEX_SERVER_URI]
    }

    val libraryKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PLEX_LIBRARY_KEY]
    }
    
    val clientIdentifier: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[CLIENT_IDENTIFIER]
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PLEX_AUTH_TOKEN] = token
        }
    }

    suspend fun saveServer(id: String, uri: String) {
        context.dataStore.edit { preferences ->
            preferences[PLEX_SERVER_ID] = id
            preferences[PLEX_SERVER_URI] = uri
        }
    }

    suspend fun saveLibrary(key: String, title: String) {
        context.dataStore.edit { preferences ->
            preferences[PLEX_LIBRARY_KEY] = key
            preferences[PLEX_LIBRARY_TITLE] = title
        }
    }
    
    suspend fun saveClientIdentifier(id: String) {
        context.dataStore.edit { preferences ->
            preferences[CLIENT_IDENTIFIER] = id
        }
    }

    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
