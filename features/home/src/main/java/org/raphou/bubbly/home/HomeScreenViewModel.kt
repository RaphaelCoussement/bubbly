package org.raphou.bubbly.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.domain.models.Theme
import org.raphou.domain.repositories.IThemeRepository

class HomeScreenViewModel : ViewModel(), KoinComponent {

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
}

