package org.raphou.bubbly.home

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.home.IUserPreferencesRepository
import org.raphou.bubbly.domain.lobby.ILobbyRepository

class LobbyContentViewModel() : ViewModel(), KoinComponent {
    private val lobbyRepository: ILobbyRepository by inject()
    private val userPreferencesRepository: IUserPreferencesRepository by inject()

    private val _codeIsValid = mutableStateOf<Boolean?>(null)
    val codeIsValid: State<Boolean?> = _codeIsValid

    fun checkLobbyCode(code: String) {
        viewModelScope.launch {
            try {
                _codeIsValid.value = lobbyRepository.doesLobbyCodeExist(code)
            } catch (e: Exception) {
                _codeIsValid.value = false
            }
        }
    }

    fun deleteLobbyByCode(code: String) {
        viewModelScope.launch {
            try {
                val player = userPreferencesRepository.getPseudo()
                if (player != null) {
                    val pseudoId = lobbyRepository.getPlayer(player)
                    lobbyRepository.deleteLobbyByCode(code, pseudoId.id)
                }
            } catch (e: Exception) {
                Log.e("LobbyViewModel", "Erreur lors de la suppression du joueur ou du lobby", e)
            }
        }
    }
}