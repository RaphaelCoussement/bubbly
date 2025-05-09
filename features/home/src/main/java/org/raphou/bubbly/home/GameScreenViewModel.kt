package org.raphou.bubbly.home

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

class GameScreenViewModel : ViewModel(), KoinComponent {
    private val lobbyRepository: ILobbyRepository by inject()
    private val userPrefs: IUserPreferencesRepository by inject()

    private val _screenState = MutableStateFlow<GameScreenState>(GameScreenState.Loading)
    val screenState: StateFlow<GameScreenState> = _screenState

    private val _navigationEvent = Channel<GameNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun init(lobbyId: String) {
        viewModelScope.launch {
            val currentPlayerPseudo = userPrefs.getPseudo()
            if (currentPlayerPseudo != null) {
                try {
                    val currentPlayer = lobbyRepository.getPlayer(currentPlayerPseudo)
                    val nextPlayerId = lobbyRepository.getPlayerIdByOrder(lobbyId)

                    when {
                        nextPlayerId == null -> {
                            _screenState.value = GameScreenState.Finish
                            _navigationEvent.send(GameNavigationEvent.NavigateToBestStory(lobbyId))
                        }
                        currentPlayer.id == nextPlayerId -> _screenState.value = GameScreenState.FirstPlayer
                        else -> _screenState.value = GameScreenState.OtherPlayer
                    }

                } catch (e: Exception) {
                    println("Erreur assignation joueur : ${e.message}")
                }
            }
        }
    }
}


sealed class GameScreenState {
    object Loading : GameScreenState()
    object FirstPlayer : GameScreenState()
    object OtherPlayer : GameScreenState()
    object Finish : GameScreenState()
}

sealed class GameNavigationEvent {
    data class NavigateToBestStory(val lobbyId: String) : GameNavigationEvent()
}


