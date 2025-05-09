package org.raphou.bubbly.game

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.home.IUserPreferencesRepository
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.word.IWordRepository

class FinalRankingScreenViewModel : ViewModel(), KoinComponent {
    private val lobbyRepository: ILobbyRepository by inject()
    private val userPrefs: IUserPreferencesRepository by inject()

    private val _ranking = mutableStateOf<List<Pair<String, Int>>>(emptyList())
    val ranking: State<List<Pair<String, Int>>> get() = _ranking

    fun fetchRanking(lobbyId: String) {
        viewModelScope.launch {
            _ranking.value = lobbyRepository.getPlayersRanking(lobbyId)
        }
    }

    suspend fun handleEndOfGame(lobbyId: String): Boolean {
        return try {
            val currentPlayerPseudo = userPrefs.getPseudo()
            if (currentPlayerPseudo == null) return false

            val currentPlayer = lobbyRepository.getPlayer(currentPlayerPseudo)
            val lastFirstPlayerId = lobbyRepository.getLastFirstPlayerId(lobbyId)

            if (currentPlayer.id == lastFirstPlayerId) {
                lobbyRepository.resetPlayersPoints(lobbyId)
                lobbyRepository.clearVotes(lobbyId)
                lobbyRepository.clearLobbyPlayers(lobbyId)
                lobbyRepository.deleteFirstPlayerOrder(lobbyId)
                lobbyRepository.deleteLobby(lobbyId)
                true // Suppressions faites
            } else {
                false // Pas autorisé à faire les suppressions
            }
        } catch (e: Exception) {
            false
        }
    }
}
