package org.raphou.bubbly.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.lobby.Lobby

class LobbyScreenViewModel : ViewModel(), KoinComponent {
    private val lobbyRepository: ILobbyRepository by inject()

    private val _currentLobby: MutableStateFlow<Lobby?> = MutableStateFlow(null)
    val currentSession: StateFlow<Lobby?>
        get() = _currentLobby

    fun createLobby() {
        viewModelScope.launch {
            val session = lobbyRepository.createLobby()
            _currentLobby.value = session
        }
    }

    fun joinLobby(code: String) {
        viewModelScope.launch {
            try {
                val session = lobbyRepository.joinLobby(code)
                _currentLobby.value = session
            } catch (e: Exception) {
                // gestion des erreurs
                e.printStackTrace()
            }
        }
    }
}