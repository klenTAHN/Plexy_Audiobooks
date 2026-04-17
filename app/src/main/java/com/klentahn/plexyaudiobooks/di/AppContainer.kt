package com.klentahn.plexyaudiobooks.di

import android.content.Context
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.local.db.AppDatabase
import com.klentahn.plexyaudiobooks.data.remote.PlexApi
import com.klentahn.plexyaudiobooks.data.repository.LibraryRepository
import com.klentahn.plexyaudiobooks.data.repository.PlexRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

import com.klentahn.plexyaudiobooks.data.MetadataMaster

class AppContainer(private val context: Context) {
    
    val metadataMaster = MetadataMaster(context)

    private val database = AppDatabase.getDatabase(context)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val plexApi = Retrofit.Builder()
        .baseUrl("https://plex.tv/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PlexApi::class.java)

    val settingsManager = SettingsManager(context)
    
    val plexRepository = PlexRepository(plexApi, settingsManager)

    val libraryRepository = LibraryRepository(
        plexApi = plexApi,
        bookDao = database.bookDao(),
        settingsManager = settingsManager,
        metadataMaster = metadataMaster
    )

    fun getPlexApi() = plexApi
}
