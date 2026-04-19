package com.klentahn.plexyaudiobooks.data.repository

import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.model.PlexDevice
import com.klentahn.plexyaudiobooks.data.model.PlexLibrary
import com.klentahn.plexyaudiobooks.data.model.PlexPinResponse
import com.klentahn.plexyaudiobooks.data.remote.PlexApi
import kotlinx.coroutines.flow.first
import java.util.UUID

class PlexRepository(
    private val plexApi: PlexApi,
    private val settingsManager: SettingsManager
) {
    private val productName = "Plexy Audiobooks"
    private val deviceName = android.os.Build.MODEL
    private val platformName = "Android"

    private suspend fun getClientIdentifier(): String {
        val currentId = settingsManager.clientIdentifier.first()
        return if (currentId != null) {
            currentId
        } else {
            val newId = UUID.randomUUID().toString()
            settingsManager.saveClientIdentifier(newId)
            newId
        }
    }

    suspend fun createPin(): PlexPinResponse? {
        val response = plexApi.createPin(
            product = productName,
            clientIdentifier = getClientIdentifier(),
            device = deviceName,
            platform = platformName
        )
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun checkPin(id: Long, code: String): PlexPinResponse? {
        val response = plexApi.checkPin(
            id = id,
            code = code,
            clientIdentifier = getClientIdentifier(),
            product = productName,
            device = deviceName,
            platform = platformName
        )
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun getServers(token: String): List<PlexDevice>? {
        val response = plexApi.getResources(
            token = token,
            clientIdentifier = getClientIdentifier(),
            product = productName,
            device = deviceName,
            platform = platformName
        )
        return if (response.isSuccessful) {
            response.body()?.filter { it.provides.contains("server") }
        } else null
    }

    suspend fun getLibraries(serverUri: String, token: String): List<PlexLibrary>? {
        val url = "$serverUri/library/sections?includePrefs=1"
        val response = plexApi.getLibrarySections(
            url = url,
            token = token,
            clientIdentifier = getClientIdentifier(),
            product = productName,
            device = deviceName,
            platform = platformName
        )
        return if (response.isSuccessful) {
            response.body()?.mediaContainer?.directories
        } else null
    }

    suspend fun getLibrary(serverUri: String, token: String, libraryKey: String): PlexLibrary? {
        val url = "$serverUri/library/sections/$libraryKey?includePrefs=1"
        val response = plexApi.getLibrarySection(
            url = url,
            token = token,
            clientIdentifier = getClientIdentifier(),
            product = productName,
            device = deviceName,
            platform = platformName
        )
        return if (response.isSuccessful) {
            val mediaContainer = response.body()?.mediaContainer
            var library = mediaContainer?.directories?.firstOrNull()
            
            if (library != null && library.enableTrackOffsets == null) {
                // If not found in directory attributes, check the settings list
                val trackOffsetsSetting = mediaContainer?.settings?.find { it.id == "enableTrackOffsets" }
                if (trackOffsetsSetting != null) {
                    val isEnabled = trackOffsetsSetting.value == "1" || 
                                    trackOffsetsSetting.value.lowercase() == "true"
                    library = library.copy(enableTrackOffsets = if (isEnabled) 1 else 0)
                }
            }
            library
        } else null
    }

    suspend fun getMetadata(serverUri: String, token: String, ratingKey: String): com.klentahn.plexyaudiobooks.data.model.PlexMetadata? {
        val url = "$serverUri/library/metadata/$ratingKey?includeExternalMedia=1&includeExtras=1&includeChapters=1&includeMarkers=1"
        val response = plexApi.getMetadata(
            url = url,
            token = token,
            clientIdentifier = getClientIdentifier(),
            product = productName,
            device = deviceName,
            platform = platformName
        )
        return if (response.isSuccessful) {
            response.body()?.mediaContainer?.metadata?.firstOrNull()
        } else null
    }

    suspend fun getChildren(serverUri: String, token: String, ratingKey: String): List<com.klentahn.plexyaudiobooks.data.model.PlexMetadata>? {
        val url = "$serverUri/library/metadata/$ratingKey/children"
        val response = plexApi.getMetadata(
            url = url,
            token = token,
            clientIdentifier = getClientIdentifier(),
            product = productName,
            device = deviceName,
            platform = platformName
        )
        return if (response.isSuccessful) {
            response.body()?.mediaContainer?.metadata
        } else null
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
    }

    suspend fun scrobble(serverUri: String, token: String, key: String) {
        val url = "$serverUri/:/scrobble"
        plexApi.scrobble(url, key, token)
    }
}
