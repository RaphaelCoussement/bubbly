package org.raphou.bubbly.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.home.IUserPreferencesRepository
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.lobby.Lobby
import org.raphou.bubbly.domain.lobby.Player
import java.util.UUID

class CreateLobbyScreenViewModel() : ViewModel(), KoinComponent {
    private val lobbyRepository: ILobbyRepository by inject()
    private val userPreferencesRepository: IUserPreferencesRepository by inject()

    private val _currentLobby: MutableStateFlow<Lobby?> = MutableStateFlow(null)
    val currentSession: StateFlow<Lobby?>
        get() = _currentLobby

    private val _players: MutableStateFlow<List<Player>> = MutableStateFlow(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _navigateToGame = MutableStateFlow<String?>(null)
    val navigateToGame: StateFlow<String?> = _navigateToGame

    private val _gameStartedEvent = Channel<Unit>(Channel.CONFLATED)
    val gameStartedEvent = _gameStartedEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            LobbyStateManager.isGameStarted.collectLatest { started ->
                if (started) {
                    _gameStartedEvent.send(Unit)  // Envoie un événement quand le jeu démarre
                }
            }
        }
    }

    fun createLobby() {
        viewModelScope.launch {
            val session = lobbyRepository.createLobby()
            _currentLobby.value = session
            lobbyRepository.listenToLobbyPlayers(session.id) { playerList ->
                _players.value = playerList
            }
        }
    }
    fun startGame() {
        viewModelScope.launch {
            val lobby = _currentLobby.value
            if (lobby != null) {
                val currentPlayers = _players.value.toMutableList()

                val player = userPreferencesRepository.getPseudo()
                if (player != null){
                    val creator = lobbyRepository.getPlayer(player)

                    if (!currentPlayers.any { it.id == creator.id }) {
                        currentPlayers.add(creator)
                        lobbyRepository.addPlayerToLobby(lobby.id, creator)
                    }
                    lobbyRepository.startLobby(lobby.id)

                    _players.value = currentPlayers
                    _navigateToGame.value = lobby.id
                    _gameStartedEvent.send(Unit)
                }
            }
        }
    }
}