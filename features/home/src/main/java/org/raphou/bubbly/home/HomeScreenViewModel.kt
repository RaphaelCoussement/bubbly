package org.raphou.bubbly.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.home.IUserPreferencesRepository
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.theme.Theme
import org.raphou.bubbly.domain.word.IWordRepository
import org.raphou.domain.repositories.IThemeRepository

class HomeScreenViewModel : ViewModel(), KoinComponent {

    private val userPreferencesRepository: IUserPreferencesRepository by inject()
    private val lobbyRepository: ILobbyRepository by inject()
    private val wordRepository: IWordRepository by inject()

    private val themeRepository: IThemeRepository by inject()

    private val _themes: MutableStateFlow<List<Theme>> = MutableStateFlow(emptyList())
    val themes: StateFlow<List<Theme>> get() = _themes

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    init {
        loadThemes()
    }

    private fun loadThemes() {
        viewModelScope.launch {
            _isLoading.value = true
            val fetchedThemes = themeRepository.getPopularThemes()
            _themes.update { fetchedThemes }
            _isLoading.value = false
        }
    }
    fun logout() {
        viewModelScope.launch {
            val pseudo = userPreferencesRepository.getPseudo()
            if (pseudo != null){
                lobbyRepository.deletePlayer(pseudo)
                wordRepository.deletePlayerWordSuggestion(pseudo)
            }
            userPreferencesRepository.clearPseudo()
        }
    }
}

