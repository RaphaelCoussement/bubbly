package org.raphou.bubbly.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _navigateToGame = MutableStateFlow<String?>(null)
    val navigateToGame: StateFlow<String?> = _navigateToGame.asStateFlow()

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

    fun monitorGameStart(code: String) {
        viewModelScope.launch {
            while (true) {
                delay(5000) // Attente de 5 secondes
                val updatedSession = lobbyRepository.getLobbyByCode(code) // Récupère les nouvelles infos
                Log.d("JoinLobbyScreenVM", "Updated session: $updatedSession")
                if (updatedSession.isStarted) {
                    _navigateToGame.value = updatedSession.id.toString()
                    break // On arrête la boucle une fois le jeu lancé
                }
            }
        }
    }

    fun listenForLobbyUpdates(lobbyId: String) {
        lobbyRepository.listenToLobbyUpdates(lobbyId) { updatedLobby ->
            _currentLobby.value = updatedLobby
            Log.d("JoinLobbyScreenVM", "🔥 Mise à jour en temps réel : ${updatedLobby.isStarted}")

            if (updatedLobby.isStarted) {
                Log.d("JoinLobbyScreenVM", "youpi")
                _navigateToGame.value = updatedLobby.id
            }
        }
    }


}
