package com.klentahn.plexyaudiobooks.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.klentahn.plexyaudiobooks.PlexyAudiobooksApplication
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.model.PlexLibrary
import com.klentahn.plexyaudiobooks.data.repository.PlexRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface LibrarySelectUiState {
    object Loading : LibrarySelectUiState
    data class Success(val libraries: List<PlexLibrary>) : LibrarySelectUiState
    data class Error(val message: String) : LibrarySelectUiState
}

class LibrarySelectViewModel(
    private val plexRepository: PlexRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibrarySelectUiState>(LibrarySelectUiState.Loading)
    val uiState: StateFlow<LibrarySelectUiState> = _uiState.asStateFlow()

    init {
        loadLibraries()
    }

    fun loadLibraries() {
        viewModelScope.launch {
            _uiState.value = LibrarySelectUiState.Loading
            val token = settingsManager.authToken.first()
            val serverUri = settingsManager.serverUri.first()
            
            if (token != null && serverUri != null) {
                val libraries = plexRepository.getLibraries(serverUri, token)
                if (libraries != null) {
                    // Filter for 'artist' libraries as music/audiobooks are identified this way in Plex API
                    val musicLibraries = libraries.filter { it.type == "artist" }
                    _uiState.value = LibrarySelectUiState.Success(musicLibraries)
                } else {
                    _uiState.value = LibrarySelectUiState.Error("Failed to load libraries")
                }
            } else {
                _uiState.value = LibrarySelectUiState.Error("Missing server or auth token")
            }
        }
    }

    fun selectLibrary(library: PlexLibrary, onSelected: (Boolean) -> Unit) {
        viewModelScope.launch {
            val token = settingsManager.authToken.first()
            val serverUri = settingsManager.serverUri.first()
            
            var isTrackProgressEnabled = library.enableTrackOffsets == 1
            
            if (token != null && serverUri != null) {
                // Fetch full library details to get the 'Store track progress' (enableTrackOffsets) setting
                val fullLibrary = plexRepository.getLibrary(serverUri, token, library.key)
                if (fullLibrary != null) {
                    isTrackProgressEnabled = fullLibrary.enableTrackOffsets == 1
                }
            }
            
            settingsManager.saveLibrary(library.key, library.title)
            onSelected(isTrackProgressEnabled)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PlexyAudiobooksApplication)
                LibrarySelectViewModel(
                    plexRepository = application.container.plexRepository,
                    settingsManager = application.container.settingsManager
                )
            }
        }
    }
}
