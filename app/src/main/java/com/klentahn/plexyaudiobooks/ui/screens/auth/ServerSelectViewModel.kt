package com.klentahn.plexyaudiobooks.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.klentahn.plexyaudiobooks.PlexyAudiobooksApplication
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.model.PlexDevice
import com.klentahn.plexyaudiobooks.data.repository.PlexRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ServerSelectUiState {
    object Loading : ServerSelectUiState
    data class Success(val servers: List<PlexDevice>) : ServerSelectUiState
    data class Error(val message: String) : ServerSelectUiState
}

class ServerSelectViewModel(
    private val plexRepository: PlexRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ServerSelectUiState>(ServerSelectUiState.Loading)
    val uiState: StateFlow<ServerSelectUiState> = _uiState.asStateFlow()

    init {
        loadServers()
    }

    fun loadServers() {
        viewModelScope.launch {
            _uiState.value = ServerSelectUiState.Loading
            val token = settingsManager.authToken.first()
            if (token != null) {
                val servers = plexRepository.getServers(token)
                if (servers != null) {
                    _uiState.value = ServerSelectUiState.Success(servers)
                } else {
                    _uiState.value = ServerSelectUiState.Error("Failed to load servers")
                }
            } else {
                _uiState.value = ServerSelectUiState.Error("No auth token found")
            }
        }
    }

    fun selectServer(device: PlexDevice, onSelected: () -> Unit) {
        viewModelScope.launch {
            // Combine both connection list variants from Plex API
            val allConnections = device.connections + device.connectionsAlt
            
            // Find the best connection (prefer local or non-relay if possible)
            val connection = allConnections.find { !it.relay } ?: allConnections.firstOrNull()
            if (connection != null) {
                settingsManager.saveServer(device.clientIdentifier, connection.uri)
                onSelected()
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PlexyAudiobooksApplication)
                ServerSelectViewModel(
                    plexRepository = application.container.plexRepository,
                    settingsManager = application.container.settingsManager
                )
            }
        }
    }
}
