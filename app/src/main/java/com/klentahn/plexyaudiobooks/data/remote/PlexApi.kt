package com.klentahn.plexyaudiobooks.data.remote

import com.klentahn.plexyaudiobooks.data.model.PlexDevice
import com.klentahn.plexyaudiobooks.data.model.PlexLibraryResponse
import com.klentahn.plexyaudiobooks.data.model.PlexPinResponse
import com.klentahn.plexyaudiobooks.data.model.PlexUserResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface PlexApi {

    @POST("https://plex.tv/api/v2/pins")
    @Headers("Accept: application/json")
    suspend fun createPin(
        @Query("strong") strong: Boolean = false,
        @Header("X-Plex-Product") product: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Device") device: String,
        @Header("X-Plex-Platform") platform: String
    ): Response<PlexPinResponse>

    @GET("https://plex.tv/api/v2/pins/{id}")
    @Headers("Accept: application/json")
    suspend fun checkPin(
        @Path("id") id: Long,
        @Query("code") code: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Product") product: String,
        @Header("X-Plex-Device") device: String,
        @Header("X-Plex-Platform") platform: String
    ): Response<PlexPinResponse>

    @GET("https://plex.tv/api/v2/user")
    @Headers("Accept: application/json")
    suspend fun getUser(
        @Header("X-Plex-Token") token: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Product") product: String,
        @Header("X-Plex-Device") device: String,
        @Header("X-Plex-Platform") platform: String
    ): Response<PlexUserResponse>

    @GET("https://plex.tv/api/v2/resources")
    @Headers("Accept: application/json")
    suspend fun getResources(
        @Query("includeHttps") includeHttps: Int = 1,
        @Header("X-Plex-Token") token: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Product") product: String,
        @Header("X-Plex-Device") device: String,
        @Header("X-Plex-Platform") platform: String
    ): Response<List<PlexDevice>>

    @GET
    @Headers("Accept: application/json")
    suspend fun getLibraries(
        @Url url: String,
        @Header("X-Plex-Token") token: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Product") product: String,
        @Header("X-Plex-Device") device: String,
        @Header("X-Plex-Platform") platform: String
    ): Response<PlexLibraryResponse>
    
    @GET
    @Headers("Accept: application/json")
    suspend fun getLibrarySections(
        @Url url: String,
        @Header("X-Plex-Token") token: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Product") product: String,
        @Header("X-Plex-Device") device: String,
        @Header("X-Plex-Platform") platform: String
    ): Response<PlexLibraryResponse>

    @GET
    @Headers("Accept: application/json")
    suspend fun getLibrarySection(
        @Url url: String,
        @Header("X-Plex-Token") token: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Product") product: String,
        @Header("X-Plex-Device") device: String,
        @Header("X-Plex-Platform") platform: String
    ): Response<PlexLibraryResponse>

    @GET
    @Headers("Accept: application/json")
    suspend fun getLibraryContents(
        @Url url: String,
        @Header("X-Plex-Token") token: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Product") product: String,
        @Header("X-Plex-Device") device: String,
        @Header("X-Plex-Platform") platform: String,
        @Query("type") type: Int? = 9,
        @Query("X-Plex-Container-Start") start: Int? = null,
        @Query("X-Plex-Container-Size") size: Int? = null
    ): Response<PlexLibraryResponse>

    @GET
    @Headers("Accept: application/json")
    suspend fun getMetadata(
        @Url url: String,
        @Header("X-Plex-Token") token: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Product") product: String,
        @Header("X-Plex-Device") device: String,
        @Header("X-Plex-Platform") platform: String
    ): Response<PlexLibraryResponse>

    @GET
    @Headers("Accept: application/json")
    suspend fun updateTimeline(
        @Url url: String,
        @Query("ratingKey") ratingKey: String,
        @Query("key") key: String,
        @Query("state") state: String,
        @Query("time") time: Long,
        @Query("duration") duration: Long,
        @Header("X-Plex-Token") token: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Product") product: String,
        @Header("X-Plex-Device") device: String,
        @Header("X-Plex-Platform") platform: String
    ): Response<Unit>

    @GET
    @Headers("Accept: application/json")
    suspend fun scrobble(
        @Url url: String,
        @Query("key") key: String,
        @Header("X-Plex-Token") token: String
    ): Response<Unit>

    @GET
    @Headers("Accept: application/json")
    suspend fun unscrobble(
        @Url url: String,
        @Query("key") key: String,
        @Header("X-Plex-Token") token: String
    ): Response<Unit>
}
