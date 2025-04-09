package org.raphou.bubbly.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.home.IUserPreferencesRepository
import org.raphou.bubbly.domain.lobby.ILobbyRepository

class ChoosePseudoScreenViewModel : ViewModel(), KoinComponent {

    private val userPreferencesRepository: IUserPreferencesRepository by inject()
    private val lobbyRepository: ILobbyRepository by inject()

    private val _pseudo = MutableStateFlow("")
    val pseudo = _pseudo.asStateFlow()

    fun onPseudoChange(newPseudo: String) {
        _pseudo.update { newPseudo }
    }

    suspend fun savePseudo(): Boolean {
        if (_pseudo.value.isBlank()) return false

        return try {
            val player = lobbyRepository.addPlayer(_pseudo.value)
            userPreferencesRepository.savePseudo(_pseudo.value)
            true
        } catch (e: Exception) {
            false
        }
    }
}