package com.klentahn.plexyaudiobooks.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.klentahn.plexyaudiobooks.PlexyAudiobooksApplication
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.local.db.BookEntity
import com.klentahn.plexyaudiobooks.data.repository.LibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder {
    AUTHOR, TITLE
}

enum class LibraryViewMode {
    TILES, LIST
}

class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.AUTHOR)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _viewMode = MutableStateFlow(LibraryViewMode.TILES)
    val viewMode: StateFlow<LibraryViewMode> = _viewMode.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val books: StateFlow<List<BookEntity>> = combine(
        settingsManager.libraryKey,
        _sortOrder,
        _searchQuery
    ) { libraryKey, sortOrder, query ->
        Triple(libraryKey, sortOrder, query)
    }.flatMapLatest { (libraryKey, sortOrder, query) ->
        if (libraryKey == null) flowOf(emptyList())
        else {
            val baseFlow = when (sortOrder) {
                SortOrder.AUTHOR -> libraryRepository.getBooksByAuthor(libraryKey)
                SortOrder.TITLE -> libraryRepository.getBooksByTitle(libraryKey)
            }
            baseFlow.map { list ->
                if (query.isBlank()) {
                    list
                } else {
                    list.filter {
                        it.title.contains(query, ignoreCase = true) ||
                                it.author.contains(query, ignoreCase = true)
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList<BookEntity>()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    init {
        refreshLibrary()
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setViewMode(mode: LibraryViewMode) {
        _viewMode.value = mode
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _isRefreshing.value = true
            libraryRepository.refreshLibrary()
            _isRefreshing.value = false
        }
    }
    
    fun refreshMetadata() {
        viewModelScope.launch {
            _isRefreshing.value = true
            libraryRepository.refreshMetadata()
            _isRefreshing.value = false
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PlexyAudiobooksApplication)
                LibraryViewModel(
                    libraryRepository = application.container.libraryRepository,
                    settingsManager = application.container.settingsManager
                )
            }
        }
    }
}
