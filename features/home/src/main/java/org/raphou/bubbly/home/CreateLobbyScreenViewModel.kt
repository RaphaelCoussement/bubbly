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

    private val _navigationEvent = Channel<CreateLobbyNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            LobbyStateManager.isGameStarted.collectLatest { started ->
                if (started) {
                    _gameStartedEvent.send(Unit)  // Envoie un événement quand le jeu démarre
                }
            }
        }
    }

    private var currentThemeId: String? = null

    fun createLobby(themeId: String?) {
        viewModelScope.launch {
            currentThemeId = themeId
            val session = lobbyRepository.createLobby(themeId)
            _currentLobby.value = session
            lobbyRepository.listenToLobbyPlayers(session.id) { playerList ->
                _players.value = playerList
            }
        }
    }
    fun startGame() {
        viewModelScope.launch {
            val lobby = _currentLobby.value

            // Vérifie d'abord si un lobby est défini localement
            if (lobby != null) {
                // Vérifie côté serveur si le lobby existe toujours
                val exists = lobbyRepository.doesLobbyCodeExist(lobby.code)
                if (!exists) {
                    _navigationEvent.send(CreateLobbyNavigationEvent.NavigateToHome)
                    return@launch
                }

                val currentPlayers = _players.value.toMutableList()
                val player = userPreferencesRepository.getPseudo()

                if (player != null) {
                    val creator = lobbyRepository.getPlayer(player)

                    if (!currentPlayers.any { it.id == creator.id }) {
                        currentPlayers.add(creator)
                        lobbyRepository.addPlayerToLobby(lobby.id, creator)
                    }

                    lobbyRepository.orderFirstPlayers(lobby.id, currentPlayers)
                    lobbyRepository.startLobby(lobby.id)

                    _players.value = currentPlayers

                    stopListeningToLobbyPlayers()

                    _navigateToGame.value = lobby.id
                    _gameStartedEvent.send(Unit)

                    _navigationEvent.send(
                        CreateLobbyNavigationEvent.NavigateToGame(lobby.id, currentThemeId)
                    )
                }
            } else {
                _navigationEvent.send(CreateLobbyNavigationEvent.NavigateToHome)
            }
        }
    }

    fun stopListeningToLobbyPlayers() {
        lobbyRepository.stopListeningToLobbyPlayers()
    }
}

sealed class CreateLobbyNavigationEvent {
    data class NavigateToGame(val lobbyId: String, val themeId: String?) : CreateLobbyNavigationEvent()
    object NavigateToHome : CreateLobbyNavigationEvent()
}
