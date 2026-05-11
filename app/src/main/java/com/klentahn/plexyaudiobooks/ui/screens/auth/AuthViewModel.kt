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

sealed interface ManualTokenState {
    object Idle : ManualTokenState
    object Loading : ManualTokenState
    object Success : ManualTokenState
    data class Error(val message: String) : ManualTokenState
}

class AuthViewModel(
    private val plexRepository: PlexRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _pinState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val pinState: StateFlow<AuthUiState> = _pinState.asStateFlow()

    private val _manualState = MutableStateFlow<ManualTokenState>(ManualTokenState.Idle)
    val manualState: StateFlow<ManualTokenState> = _manualState.asStateFlow()

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
        val clientId = plexRepository.getClientIdentifierBlocking()
        val product = URLEncoder.encode(plexRepository.productName, "UTF-8")
        val device = URLEncoder.encode(android.os.Build.MODEL, "UTF-8")
        val encodedClientId = URLEncoder.encode(clientId, "UTF-8")
        val encodedCode = URLEncoder.encode(pin.code, "UTF-8")
        val forwardUrl = URLEncoder.encode("plexy://auth", "UTF-8")

        // Plex V2 Auth URL with identification in both fragment and query parameters
        // to maximize compatibility with the web app and sub-requests.
        return "https://app.plex.tv/auth#?" +
                "clientID=$encodedClientId" +
                "&code=$encodedCode" +
                "&X-Plex-Client-Identifier=$encodedClientId" +
                "&X-Plex-Product=$product" +
                "&X-Plex-Device=$device" +
                "&X-Plex-Platform=Android" +
                "&context%5Bdevice%5D%5Bproduct%5D=$product" +
                "&context%5Bdevice%5D%5Bplatform%5D=Android" +
                "&context%5Bdevice%5D%5Bdevice%5D=$device" +
                "&forwardUrl=$forwardUrl"
    }

    private fun startPolling(pin: PlexPinResponse) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repeat(90) {  // 3 minutes
                checkPinStatus()
                if (_pinState.value is AuthUiState.Success) return@launch
                delay(1500)   // Check every 1.5 seconds
            }
        }
    }

    fun checkPinStatus() {
        val current = _pinState.value
        if (current !is AuthUiState.PinGenerated) return

        viewModelScope.launch {
            try {
                val checked = plexRepository.checkPin(current.pin.id, current.pin.code)
                if (checked?.authToken != null) {
                    settingsManager.saveAuthToken(checked.authToken)
                    _pinState.value = AuthUiState.Success
                    pollingJob?.cancel()
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is java.net.UnknownHostException -> "No internet connection to Plex. Please check Wi-Fi / Mobile data."
                    else -> "Connection error: ${e.message}"
                }
                _pinState.value = AuthUiState.Error(msg)
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