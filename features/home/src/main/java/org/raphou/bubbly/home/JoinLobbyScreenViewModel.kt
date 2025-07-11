package org.raphou.bubbly.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.home.IUserPreferencesRepository
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.lobby.Lobby
import org.raphou.bubbly.domain.lobby.Player
import java.util.UUID

class JoinLobbyScreenViewModel() : ViewModel(), KoinComponent {

    private val lobbyRepository: ILobbyRepository by inject()

    private val userPreferencesRepository: IUserPreferencesRepository by inject()

    private val _currentLobby: MutableStateFlow<Lobby?> = MutableStateFlow(null)
    val currentSession: StateFlow<Lobby?> = _currentLobby.asStateFlow()

    private val _players: MutableStateFlow<List<Player>> = MutableStateFlow(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _navigateToGameChannel = Channel<JoinLobbyNavigationEvent>()
    val navigateToGameChannel = _navigateToGameChannel.receiveAsFlow()

    fun joinLobby(code: String) {
        viewModelScope.launch {
            try {
                val pseudo = userPreferencesRepository.getPseudo()
                Log.d("JoinLobbyScreenVM", "🔥 pseudo : ${pseudo}")
                if (pseudo != null) {
                    val session = lobbyRepository.joinLobby(code, pseudo)
                    _currentLobby.value = session

                    // Écoute les joueurs
                    lobbyRepository.listenToLobbyPlayers(session.id) { playerList ->
                        _players.value = playerList
                    }

                    // Lance la vérification périodique
                    listenForLobbyUpdates(session.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun listenForLobbyUpdates(lobbyId: String) {
        lobbyRepository.listenToLobbyUpdates(lobbyId) { updatedLobby ->
            _currentLobby.value = updatedLobby
            Log.d("JoinLobbyScreenVM", "🔥 Mise à jour en temps réel : ${updatedLobby.isStarted}")

            if (updatedLobby.isStarted) {
                Log.d("JoinLobbyScreenVM", "youpi")
                stopListeningToLobbyPlayers()

                // Lancer une coroutine ici
                viewModelScope.launch {
                    val themeId = try {
                        lobbyRepository.getLobbyByCode(updatedLobby.code).themeId ?: "default"
                    } catch (e: Exception) {
                        "default"
                    }

                    _navigateToGameChannel.trySend(
                        JoinLobbyNavigationEvent.NavigateToGame(updatedLobby.id, themeId)
                    )
                }
            }
        }
    }

    fun stopListeningToLobbyPlayers() {
        // arrete l'écoute une fois que l'on navigue
        lobbyRepository.stopListeningToLobbyPlayers()
    }

    suspend fun getThemeIdByCode(code: String): String {
        return try {
            val lobby = lobbyRepository.getLobbyByCode(code)
            lobby.themeId ?: "default"
        } catch (e: Exception) {
            Log.e("JoinLobbyScreenVM", "Erreur lors de la récupération du thème", e)
            "default"
        }
    }
}

sealed class JoinLobbyNavigationEvent {
    data class NavigateToGame(val lobbyId: String, val themeId: String) : JoinLobbyNavigationEvent()
}

