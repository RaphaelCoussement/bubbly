package org.raphou.bubbly.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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


    fun init(lobbyId: String) {
        viewModelScope.launch {
            val currentPlayer = userPrefs.getPseudo()
            if (currentPlayer != null) {
                try {
                    val firstPlayerName = lobbyRepository.assignFirstPlayerIfNeeded(lobbyId, currentPlayer)
                    when (firstPlayerName) {
                        "final ranking" -> _screenState.value = GameScreenState.Finish
                        currentPlayer -> _screenState.value = GameScreenState.FirstPlayer
                        else -> _screenState.value = GameScreenState.OtherPlayer
                    }
                } catch (e: Exception) {
                    println("Erreur assignation premier joueur : ${e.message}")
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

