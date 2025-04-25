package org.raphou.bubbly.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.home.IUserPreferencesRepository

class SplashScreenViewModel : ViewModel(), KoinComponent {

    private val userPreferencesRepository: IUserPreferencesRepository by inject()

    private val _pseudo = MutableStateFlow<String?>(null)
    val pseudo = _pseudo.asStateFlow()

    suspend fun loadPseudo() {
        val savedPseudo = userPreferencesRepository.getPseudo()
        _pseudo.emit(savedPseudo)
    }
}