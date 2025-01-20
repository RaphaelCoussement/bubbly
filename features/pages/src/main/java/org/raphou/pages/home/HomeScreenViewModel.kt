package org.raphou.pages.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.raphou.domain.models.Theme
import org.raphou.domain.repositories.IThemeRepository

class HomeScreenViewModel(private val themeRepository: IThemeRepository) : ViewModel() {

    private val _themes = MutableStateFlow<List<Theme>>(emptyList())
    val themes: StateFlow<List<Theme>> = _themes

    init {
        loadThemes()
    }

    private fun loadThemes() {
        viewModelScope.launch {
            _themes.value = themeRepository.getPopularThemes()
            Log.d("HomeViewModel", "Fetched Themes: ${_themes.value}")
        }
    }
}