package com.klentahn.plexyaudiobooks.data.repository

import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.model.PlexDevice
import com.klentahn.plexyaudiobooks.data.model.PlexLibrary
import com.klentahn.plexyaudiobooks.data.model.PlexMetadata
import com.klentahn.plexyaudiobooks.data.model.PlexPinResponse
import com.klentahn.plexyaudiobooks.data.remote.PlexApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.UUID

class PlexRepository(
    private val plexApi: PlexApi,
    private val settingsManager: SettingsManager
) {
    val productName = "Plexy Audiobooks"
    val deviceName = android.os.Build.MODEL
    val platformName = "Android"

    suspend fun getClientIdentifier(): String {
        val currentId = settingsManager.clientIdentifier.first()
        return if (currentId != null) {
            currentId
        } else {
            val newId = UUID.randomUUID().toString()
            settingsManager.saveClientIdentifier(newId)
            newId
        }
    }

    fun getClientIdentifierBlocking(): String {
        return runBlocking { getClientIdentifier() }
    }

    suspend fun createPin(): PlexPinResponse? {
        val clientId = getClientIdentifier()
        android.util.Log.d("PlexRepository", "createPin: clientId=$clientId")
        return try {
            val response = plexApi.createPin(
                product = productName,
                clientIdentifier = clientId,
                device = deviceName,
                platform = platformName
            )
            if (response.isSuccessful) {
                response.body()
            } else {
                android.util.Log.e("PlexRepository", "createPin failed: ${response.code()} ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("PlexRepository", "createPin exception", e)
            null
        }
    }

    suspend fun checkPin(id: Long): PlexPinResponse? {
        return try {
            val response = plexApi.checkPin(
                id = id,
                clientIdentifier = getClientIdentifier(),
                product = productName,
                device = deviceName,
                platform = platformName
            )
            if (response.isSuccessful) {
                response.body()
            } else {
                android.util.Log.e("PlexRepository", "checkPin failed: ${response.code()} ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("PlexRepository", "checkPin exception: ${e.message}", e)
            null
        }
    }

    suspend fun validateToken(token: String): Boolean {
        return try {
            val response = plexApi.getUser(
                token = token,
                clientIdentifier = getClientIdentifier(),
                product = productName,
                device = deviceName,
                platform = platformName
            )
            response.isSuccessful && response.body() != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getServers(token: String): List<PlexDevice>? {
        return try {
            val response = plexApi.getResources(
                token = token,
                clientIdentifier = getClientIdentifier(),
                product = productName,
                device = deviceName,
                platform = platformName
            )
            if (response.isSuccessful) {
                // Filter for 'server' type resources
                response.body()?.filter { it.provides.contains("server") }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLibraries(serverUri: String, token: String): List<PlexLibrary>? {
        return try {
            val url = "$serverUri/library/sections"
            val response = plexApi.getLibraries(
                url = url,
                token = token,
                clientIdentifier = getClientIdentifier(),
                product = productName,
                device = deviceName,
                platform = platformName
            )
            if (response.isSuccessful) {
                response.body()?.mediaContainer?.directories
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLibrary(serverUri: String, token: String, libraryKey: String): PlexLibrary? {
        return try {
            val url = "$serverUri/library/sections/$libraryKey"
            val response = plexApi.getLibrarySection(
                url = url,
                token = token,
                clientIdentifier = getClientIdentifier(),
                product = productName,
                device = deviceName,
                platform = platformName
            )
            if (response.isSuccessful) {
                response.body()?.mediaContainer?.directories?.firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMetadata(serverUri: String, token: String, ratingKey: String): PlexMetadata? {
        val url = "$serverUri/library/metadata/$ratingKey?includeExternalMedia=1&includeExtras=1&includeChapters=1"
        return getMetadataByUrl(url, token)
    }

    suspend fun getMetadataByUrl(url: String, token: String): PlexMetadata? {
        return try {
            val response = plexApi.getMetadata(
                url = url,
                token = token,
                clientIdentifier = getClientIdentifier(),
                product = productName,
                device = deviceName,
                platform = platformName
            )
            if (response.isSuccessful) {
                response.body()?.mediaContainer?.metadata?.firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getChildren(serverUri: String, token: String, ratingKey: String): List<PlexMetadata>? {
        return try {
            val url = "$serverUri/library/metadata/$ratingKey/children"
            val response = plexApi.getMetadata(
                url = url,
                token = token,
                clientIdentifier = getClientIdentifier(),
                product = productName,
                device = deviceName,
                platform = platformName
            )
            if (response.isSuccessful) {
                response.body()?.mediaContainer?.metadata
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateTimeline(
        serverUri: String,
        token: String,
        ratingKey: String,
        key: String,
        state: String,
        time: Long,
        duration: Long
    ) {
        try {
            val url = "$serverUri/:/timeline"
            plexApi.updateTimeline(
                url = url,
                ratingKey = ratingKey,
                key = key,
                state = state,
                time = time,
                duration = duration,
                token = token,
                clientIdentifier = getClientIdentifier(),
                product = productName,
                device = deviceName,
                platform = platformName
            )
        } catch (_: Exception) {
        }
    }

    suspend fun scrobble(serverUri: String, token: String, key: String) {
        try {
            val url = "$serverUri/:/scrobble"
            plexApi.scrobble(url = url, key = key, token = token)
        } catch (_: Exception) {
        }
    }

    suspend fun signOut() {
        settingsManager.clearAuth()
    }
}
