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
import org.raphou.bubbly.domain.lobby.Player
import java.util.UUID

class LobbyScreenViewModel : ViewModel(), KoinComponent {
    private val lobbyRepository: ILobbyRepository by inject()

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
                val session = lobbyRepository.joinLobby(code)
                _currentLobby.value = session
                lobbyRepository.listenToLobbyPlayers(session.id) { playerList ->
                    _players.value = playerList
                }
            } catch (e: Exception) {
                // Gestion des erreurs
                e.printStackTrace()
            }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            val lobby = _currentLobby.value
            if (lobby != null) {
                val currentPlayers = _players.value.toMutableList()

                val creator = Player(id = UUID.randomUUID().toString(), name = "Créateur")
                if (!currentPlayers.any { it.id == creator.id }) {
                    currentPlayers.add(creator)
                    lobbyRepository.addPlayerToLobby(lobby.id, creator)
                }

                // Pour simplifier, le premier joueur est le créateur
                lobbyRepository.setFirstPlayer(lobby.id, creator.id)

                _players.value = currentPlayers
                _navigateToGame.value = lobby.id
                _isGameStarted.value = true
            }
        }
    }
}

