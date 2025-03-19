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
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.lobby.Lobby
import org.raphou.bubbly.domain.lobby.Player
import java.util.UUID

class JoinLobbyScreenViewModel() : ViewModel(), KoinComponent {

    private val lobbyRepository: ILobbyRepository by inject()

    private val _currentLobby: MutableStateFlow<Lobby?> = MutableStateFlow(null)
    val currentSession: StateFlow<Lobby?> = _currentLobby.asStateFlow()

    private val _players: MutableStateFlow<List<Player>> = MutableStateFlow(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _navigateToGameChannel = Channel<String?>()
    val navigateToGameChannel = _navigateToGameChannel.receiveAsFlow()

    fun joinLobby(code: String) {
        viewModelScope.launch {
            try {
                val session = lobbyRepository.joinLobby(code)
                _currentLobby.value = session

                // Écoute les joueurs
                lobbyRepository.listenToLobbyPlayers(session.id) { playerList ->
                    _players.value = playerList
                }

                // Lance la vérification périodique
                listenForLobbyUpdates(session.id)
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
                // Envoie l'ID du lobby dans le channel
                _navigateToGameChannel.trySend(updatedLobby.id)
            }
        }
    }
}
