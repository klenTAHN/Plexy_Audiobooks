package com.klentahn.plexyaudiobooks.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.klentahn.plexyaudiobooks.PlexyAudiobooksApplication
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.repository.LibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class AuthorsViewModel(
    private val libraryRepository: LibraryRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val authors: StateFlow<List<String>> = settingsManager.libraryKey.flatMapLatest { libraryKey ->
        if (libraryKey == null) flowOf(emptyList())
        else libraryRepository.getAuthors(libraryKey)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PlexyAudiobooksApplication)
                AuthorsViewModel(
                    libraryRepository = application.container.libraryRepository,
                    settingsManager = application.container.settingsManager
                )
            }
        }
    }
}
