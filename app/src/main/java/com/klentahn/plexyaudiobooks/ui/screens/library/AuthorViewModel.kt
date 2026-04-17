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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class AuthorViewModel(
    private val author: String,
    private val libraryRepository: LibraryRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val books: StateFlow<List<BookEntity>> = settingsManager.libraryKey
        .flatMapLatest { libraryKey ->
            if (libraryKey == null) flowOf(emptyList())
            else libraryRepository.getBooksByAuthorFiltered(author, libraryKey)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    companion object {
        fun provideFactory(author: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PlexyAudiobooksApplication)
                AuthorViewModel(
                    author = author,
                    libraryRepository = application.container.libraryRepository,
                    settingsManager = application.container.settingsManager
                )
            }
        }
    }
}
