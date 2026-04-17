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
            clientIdentifier = getClientIdentifier()
        )
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun checkPin(id: Long, code: String): PlexPinResponse? {
        val response = plexApi.checkPin(
            id = id,
            code = code,
            clientIdentifier = getClientIdentifier()
        )
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun getServers(token: String): List<PlexDevice>? {
        val response = plexApi.getResources(
            token = token,
            clientIdentifier = getClientIdentifier()
        )
        return if (response.isSuccessful) {
            response.body()?.filter { it.provides.contains("server") }
        } else null
    }

    suspend fun getLibraries(serverUri: String, token: String): List<PlexLibrary>? {
        val url = "$serverUri/library/sections"
        val response = plexApi.getLibrarySections(url, token)
        return if (response.isSuccessful) {
            response.body()?.mediaContainer?.directories
        } else null
    }

    suspend fun getLibrary(serverUri: String, token: String, libraryKey: String): PlexLibrary? {
        val url = "$serverUri/library/sections/$libraryKey"
        val response = plexApi.getLibrarySection(url, token)
        return if (response.isSuccessful) {
            response.body()?.mediaContainer?.directories?.firstOrNull()
        } else null
    }

    suspend fun getMetadata(serverUri: String, token: String, ratingKey: String): com.klentahn.plexyaudiobooks.data.model.PlexMetadata? {
        val url = "$serverUri/library/metadata/$ratingKey?includeExternalMedia=1&includeExtras=1&includeChapters=1&includeMarkers=1"
        val response = plexApi.getMetadata(url, token)
        return if (response.isSuccessful) {
            response.body()?.mediaContainer?.metadata?.firstOrNull()
        } else null
    }

    suspend fun getChildren(serverUri: String, token: String, ratingKey: String): List<com.klentahn.plexyaudiobooks.data.model.PlexMetadata>? {
        val url = "$serverUri/library/metadata/$ratingKey/children"
        val response = plexApi.getMetadata(url, token)
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
            product = productName
        )
    }

    suspend fun scrobble(serverUri: String, token: String, key: String) {
        val url = "$serverUri/:/scrobble"
        plexApi.scrobble(url, key, token)
    }
}
