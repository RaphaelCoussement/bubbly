package org.raphou.bubbly.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.lobby.Lobby
import org.raphou.bubbly.domain.lobby.Player
import java.util.UUID

class JoinLobbyScreenViewModel() : ViewModel(), KoinComponent {

    private val lobbyRepository: ILobbyRepository by inject()

    private val _currentLobby: MutableStateFlow<Lobby?> = MutableStateFlow(null)
    val currentSession: StateFlow<Lobby?> = _currentLobby

    private val _players: MutableStateFlow<List<Player>> = MutableStateFlow(emptyList())
    val players: StateFlow<List<Player>> = _players

    val isGameStarted: StateFlow<Boolean> = LobbyStateManager.isGameStarted

    fun joinLobby(code: String) {
        viewModelScope.launch {
            try {
                val session = lobbyRepository.joinLobby(code)
                _currentLobby.value = session
                lobbyRepository.listenToLobbyPlayers(session.id) { playerList ->
                    _players.value = playerList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
