package org.raphou.bubbly.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.home.IUserPreferencesRepository
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.lobby.Lobby
import org.raphou.bubbly.domain.lobby.Player
import java.util.UUID

class LobbyScreenViewModel : ViewModel(), KoinComponent {
    private val lobbyRepository: ILobbyRepository by inject()
    private val userPreferencesRepository: IUserPreferencesRepository by inject()

    private val _currentLobby: MutableStateFlow<Lobby?> = MutableStateFlow(null)
    val currentSession: StateFlow<Lobby?>
        get() = _currentLobby

    private val _players: MutableStateFlow<List<Player>> = MutableStateFlow(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _navigateToGame = MutableStateFlow<String?>(null)
    val navigateToGame: StateFlow<String?> = _navigateToGame

    private val _isGameStarted = MutableStateFlow(false)
    val isGameStarted: StateFlow<Boolean> = _isGameStarted

    fun createLobby() {
        viewModelScope.launch {
            val session = lobbyRepository.createLobby()
            _currentLobby.value = session
            lobbyRepository.listenToLobbyPlayers(session.id) { playerList ->
                _players.value = playerList
            }
        }
    }

    fun joinLobby(code: String) {
        viewModelScope.launch {
            try {
                val player = userPreferencesRepository.getPseudo()

                if (player != null){
                    val session = lobbyRepository.joinLobby(code, player)
                    _currentLobby.value = session
                    lobbyRepository.listenToLobbyPlayers(session.id) { playerList ->
                        _players.value = playerList
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

