package org.raphou.bubbly.game

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.word.IWordRepository

class RankingScreenViewModel : ViewModel(), KoinComponent {
    private val lobbyRepository: ILobbyRepository by inject()
    private val wordRepository: IWordRepository by inject()

    private val _ranking = mutableStateOf<List<Pair<String, Int>>>(emptyList())
    val ranking: State<List<Pair<String, Int>>> get() = _ranking

    fun fetchRanking(lobbyId: String) {
        viewModelScope.launch {
            _ranking.value = lobbyRepository.getPlayersRanking(lobbyId)
        }
    }

    fun resetLobbyAfterRanking(lobbyId: String) {
        viewModelScope.launch {
            lobbyRepository.resetLobby(lobbyId)
            wordRepository.clearPlayerSuggestions(lobbyId)
            wordRepository.clearSelectedWords(lobbyId)
        }
    }

}
