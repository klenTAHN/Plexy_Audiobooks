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
import java.net.URLEncoder

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class PinGenerated(val pin: PlexPinResponse, val authUrl: String) : AuthUiState
    object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val plexRepository: PlexRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _pinState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val pinState: StateFlow<AuthUiState> = _pinState.asStateFlow()

    private var pollingJob: Job? = null

    fun generatePin() {
        viewModelScope.launch {
            _pinState.value = AuthUiState.Loading
            try {
                val pin = plexRepository.createPin()
                if (pin != null) {
                    val authUrl = buildAuthUrl(pin)
                    _pinState.value = AuthUiState.PinGenerated(pin, authUrl)

                    // Open browser immediately
                    // Note: We can't open from ViewModel easily, so it's handled in Screen

                    startPolling(pin)
                } else {
                    _pinState.value = AuthUiState.Error("Failed to generate PIN")
                }
            } catch (e: Exception) {
                _pinState.value = AuthUiState.Error("Network error: ${e.message}")
            }
        }
    }

    private fun buildAuthUrl(pin: PlexPinResponse): String {
        // The original method directing users to plex.tv/link to enter the code manually
        return "https://plex.tv/link"
    }

    private fun startPolling(pin: PlexPinResponse) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            // Poll for 15 minutes (max PIN life)
            val maxAttempts = (15 * 60) / 2 // every 2 seconds
            repeat(maxAttempts) {
                delay(2000)   // Check every 2 seconds
                checkPinStatus()
                if (_pinState.value is AuthUiState.Success) return@launch
            }
            _pinState.value = AuthUiState.Error("PIN expired. Please try again.")
        }
    }

    fun checkPinStatus(isManual: Boolean = false) {
        val current = _pinState.value
        if (current !is AuthUiState.PinGenerated) {
            android.util.Log.d("AuthViewModel", "checkPinStatus: Not in PinGenerated state. Current: ${current::class.simpleName}")
            return
        }

        viewModelScope.launch {
            try {
                if (isManual) _pinState.value = AuthUiState.Loading // Show loading for manual check
                
                android.util.Log.d("AuthViewModel", "checkPinStatus: Checking for ID=${current.pin.id} (Manual: $isManual)")
                val checked = plexRepository.checkPin(current.pin.id)
                android.util.Log.d("AuthViewModel", "checkPinStatus: Result - authToken=${checked?.authToken?.take(5)}...")
                
                if (checked?.authToken != null) {
                    settingsManager.saveAuthToken(checked.authToken)
                    _pinState.value = AuthUiState.Success
                    pollingJob?.cancel()
                } else if (isManual) {
                    // If manual and no token, go back to PinGenerated so they can try again
                    _pinState.value = current
                }
            } catch (e: Exception) {
                // Log the error
                android.util.Log.w("AuthViewModel", "checkPinStatus: Error: ${e.message}")
                if (isManual) {
                    _pinState.value = AuthUiState.Error("Connection failed: ${e.message}. Please check internet and try again.")
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
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PlexyAudiobooksApplication
                AuthViewModel(
                    plexRepository = app.container.plexRepository,
                    settingsManager = app.container.settingsManager
                )
            }
        }
    }
}