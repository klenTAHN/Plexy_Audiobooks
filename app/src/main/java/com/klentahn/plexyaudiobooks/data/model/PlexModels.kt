package com.klentahn.plexyaudiobooks.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlexPinResponse(
    val id: Long,
    val code: String,
    @Json(name = "product") val product: String,
    @Json(name = "expiresIn") val expiresIn: Int,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "authToken") val authToken: String? = null
)

@JsonClass(generateAdapter = true)
data class PlexUserResponse(
    val id: Long,
    val uuid: String,
    val username: String,
    val email: String,
    val thumb: String?,
    @Json(name = "authToken") val authToken: String
)

@JsonClass(generateAdapter = true)
data class PlexDevice(
    val name: String = "",
    val product: String = "",
    val productVersion: String = "",
    val platform: String = "",
    val platformVersion: String = "",
    val device: String = "",
    val clientIdentifier: String = "",
    val createdAt: String = "",
    val lastSeenAt: String = "",
    val provides: String = "",
    val owned: Boolean = false,
    val publicAddress: String = "",
    val httpsRequired: Boolean = false,
    val synced: Boolean = false,
    val relay: Boolean = false,
    val dnsRebindingProtection: Boolean = false,
    val natLoopbackSupported: Boolean = false,
    val publicAddressMatches: Boolean = false,
    val presence: Boolean = false,
    val accessToken: String? = null,
    @Json(name = "connections") val connections: List<PlexConnection> = emptyList(),
    @Json(name = "Connection") val connectionsAlt: List<PlexConnection> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlexConnection(
    val protocol: String = "",
    val address: String = "",
    val port: Int = 0,
    val uri: String = "",
    val local: Boolean = false,
    val relay: Boolean = false,
    val IPv6: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PlexLibraryResponse(
    @Json(name = "MediaContainer") val mediaContainer: MediaContainer
)

@JsonClass(generateAdapter = true)
data class MediaContainer(
    val size: Int,
    val totalSize: Int? = null,
    val offset: Int? = null,
    @Json(name = "Directory") val directories: List<PlexLibrary>? = null,
    @Json(name = "Metadata") val metadata: List<PlexMetadata>? = null
)

@JsonClass(generateAdapter = true)
data class PlexLibrary(
    val key: String = "",
    val type: String = "",
    val title: String = "",
    val agent: String = "",
    val scanner: String = "",
    val language: String = "",
    val uuid: String = "",
    val updatedAt: Long = 0,
    val createdAt: Long = 0,
    val scannedAt: Long = 0,
    val content: Boolean = false,
    val directory: Boolean = false,
    val contentChangedAt: Long = 0,
    val hidden: Int = 0,
    val enableTrackOffsets: Int? = null,
    @Json(name = "Location") val locations: List<PlexLocation>? = null
)

@JsonClass(generateAdapter = true)
data class PlexLocation(
    val id: Int,
    val path: String
)

@JsonClass(generateAdapter = true)
data class PlexMetadata(
    val ratingKey: String,
    val key: String,
    val guid: String,
    val type: String,
    val title: String,
    val titleSort: String? = null,
    val summary: String? = null,
    val thumb: String? = null,
    val art: String? = null,
    val duration: Long? = null,
    val addedAt: Long? = null,
    val updatedAt: Long? = null,
    val parentTitle: String? = null, // Author for Books
    val parentRatingKey: String? = null,
    val parentThumb: String? = null,
    val grandparentTitle: String? = null,
    val grandparentThumb: String? = null,
    val year: Int? = null,
    val index: Int? = null,
    val leafCount: Int? = null,
    val viewedLeafCount: Int? = null,
    val viewCount: Int? = null,
    val lastViewedAt: Long? = null,
    val viewOffset: Long? = null,
    @Json(name = "Chapter") val chapters: List<PlexChapter>? = null,
    @Json(name = "Media") val media: List<PlexMedia>? = null
)

@JsonClass(generateAdapter = true)
data class PlexChapter(
    val id: Long? = null,
    val filter: String? = null,
    val tag: String? = null,
    val index: Int? = null,
    val startTimeOffset: Long? = null,
    val endTimeOffset: Long? = null,
    val thumb: String? = null
)

@JsonClass(generateAdapter = true)
data class PlexMedia(
    val id: Long,
    val duration: Long,
    val bitrate: Int,
    val width: Int? = null,
    val height: Int? = null,
    val aspectRatio: Double? = null,
    val audioChannels: Int,
    val audioCodec: String,
    val videoCodec: String? = null,
    val container: String,
    @Json(name = "Part") val parts: List<PlexPart>
)

@JsonClass(generateAdapter = true)
data class PlexPart(
    val id: Long,
    val key: String,
    val duration: Long,
    val file: String,
    val size: Long,
    val container: String
)
