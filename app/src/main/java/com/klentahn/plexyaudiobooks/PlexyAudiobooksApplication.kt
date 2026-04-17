package com.klentahn.plexyaudiobooks

import android.app.Application
import com.klentahn.plexyaudiobooks.di.AppContainer

class PlexyAudiobooksApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
