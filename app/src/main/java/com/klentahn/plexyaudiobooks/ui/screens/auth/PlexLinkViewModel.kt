package com.klentahn.plexyaudiobooks.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.klentahn.plexyaudiobooks.PlexyAudiobooksApplication
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.model.PlexPinResponse
import com.klentahn.plexyaudiobooks.data.repository.PlexRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlexLinkUiState {
    object Idle : PlexLinkUiState
    object Loading : PlexLinkUiState
    data class PinGenerated(val pin: PlexPinResponse) : PlexLinkUiState
    object Success : PlexLinkUiState
    data class Error(val message: String) : PlexLinkUiState
}

class PlexLinkViewModel(
    private val plexRepository: PlexRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlexLinkUiState>(PlexLinkUiState.Idle)
    val uiState: StateFlow<PlexLinkUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun generatePin() {
        viewModelScope.launch {
            _uiState.value = PlexLinkUiState.Loading
            val pin = plexRepository.createPin()
            if (pin != null) {
                _uiState.value = PlexLinkUiState.PinGenerated(pin)
                startPolling(pin)
            } else {
                _uiState.value = PlexLinkUiState.Error("Failed to generate PIN")
            }
        }
    }

    private fun startPolling(pin: PlexPinResponse) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(5000) // Poll every 5 seconds
                val checkedPin = plexRepository.checkPin(pin.id, pin.code)
                if (checkedPin?.authToken != null) {
                    settingsManager.saveAuthToken(checkedPin.authToken)
                    _uiState.value = PlexLinkUiState.Success
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PlexyAudiobooksApplication)
                PlexLinkViewModel(
                    plexRepository = application.container.plexRepository,
                    settingsManager = application.container.settingsManager
                )
            }
        }
    }
}
