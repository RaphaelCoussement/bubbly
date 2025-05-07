package org.raphou.bubbly.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.home.IUserPreferencesRepository
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.lobby.Player

class PlayersVoteScreenViewModel : ViewModel(), KoinComponent {
    private val lobbyRepository: ILobbyRepository by inject()
    private val userPrefs: IUserPreferencesRepository by inject()

    private val _votes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val votes: StateFlow<Map<String, Int>> get() = _votes

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> get() = _players

    private val _allVotesReceived = MutableStateFlow(false)
    val allVotesReceived: StateFlow<Boolean> get() = _allVotesReceived

    private val _uiEventChannel = Channel<PlayersVoteUiEvent>()
    val uiEvent = _uiEventChannel.receiveAsFlow()


    fun listenToVotes(lobbyId: String) {
        lobbyRepository.listenToVotes(lobbyId) { votesMap, allVotes ->
            _votes.value = votesMap
            _allVotesReceived.value = allVotes

            if (allVotes) {
                viewModelScope.launch {
                    // Envoie l'événement de navigation lorsque tous les votes sont reçus
                    _uiEventChannel.send(PlayersVoteUiEvent.NavigateToFinalRanking)
                }
            }
        }
    }


    fun listenToLobbyPlayers(lobbyId: String) {
        lobbyRepository.listenToLobbyPlayers(lobbyId) { playersList ->
            _players.value = playersList
        }
    }

    fun voteForPlayer(lobbyId: String, votedPlayerId: String) {
        viewModelScope.launch {
            val voterPseudo = userPrefs.getPseudo()
            if (voterPseudo != null) {
                val voter = lobbyRepository.getPlayer(voterPseudo)
                lobbyRepository.voteForPlayer(lobbyId, voter.id, votedPlayerId)
            }
        }
    }

    fun resetVotes(lobbyId: String) {
        viewModelScope.launch {
            lobbyRepository.resetVotes(lobbyId)
        }
    }

    fun addPointsToWinners(lobbyId: String, winners: List<String>) {
        viewModelScope.launch {
            lobbyRepository.addPointsToWinners(lobbyId, winners)
        }
    }
}

sealed class PlayersVoteUiEvent {
    object NavigateToFinalRanking : PlayersVoteUiEvent()
}

