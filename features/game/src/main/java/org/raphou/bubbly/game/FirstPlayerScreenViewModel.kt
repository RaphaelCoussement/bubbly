package org.raphou.bubbly.game

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.home.IUserPreferencesRepository
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.lobby.Player
import org.raphou.bubbly.domain.word.IWordRepository
import org.raphou.bubbly.domain.word.Word

class FirstPlayerScreenViewModel : ViewModel(), KoinComponent {
    private val wordRepository: IWordRepository by inject()
    private val lobbyRepository: ILobbyRepository by inject()
    private val userPrefs: IUserPreferencesRepository by inject()

    private val _words = mutableStateOf<List<Word>>(emptyList())
    val words: State<List<Word>> get() = _words

    private val _foundWords = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val foundWords: StateFlow<Map<String, Boolean>> get() = _foundWords

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> get() = _score


    fun fetchWords(lobbyId: String, themeId: String?) {
        Log.d("fetchWords", "themeID : $themeId")
        viewModelScope.launch {
            // Si themeId est "default", on le remplace par null
            val finalThemeId = if (themeId == "default") null else themeId

            // On passe le thème modifié à la fonction getRandomWords
            wordRepository.initializeWords()
            val selectedWords = wordRepository.getRandomWords(finalThemeId, lobbyId)
            _words.value = selectedWords
            checkWordStatus(lobbyId)
        }
    }

    suspend fun checkWordStatus(lobbyId: String) {
        val currentPlayer = userPrefs.getPseudo()
        if (currentPlayer != null){
            val player = lobbyRepository.getPlayer(currentPlayer)
            viewModelScope.launch {
                val statusMap = mutableMapOf<String, Boolean>()
                _words.value.forEach { word ->
                    val wordInfo = wordRepository.getWordInfo(lobbyId, player.id, word.name)
                    statusMap[word.name] = wordInfo?.isFound ?: false
                }
                _foundWords.value = statusMap.toMap()
            }
        }
    }

    suspend fun fetchFinalScore(lobbyId: String) {
        val currentPlayer = userPrefs.getPseudo()
        if (currentPlayer != null){
            val player = lobbyRepository.getPlayer(currentPlayer)
            viewModelScope.launch {
                _score.value = wordRepository.getTotalPoints(lobbyId, player.id)
            }
        }
    }
    fun setIsTimeFinished(lobbyId: String){
        viewModelScope.launch {
            lobbyRepository.setIsTimeFinished(lobbyId)
        }
    }

    fun isTimeStarted(lobbyId: String){
        viewModelScope.launch {
            lobbyRepository.isTimeStarted(lobbyId)
        }
    }

    fun onPlayerTurnFinished(lobbyId: String) {
        viewModelScope.launch {
            try {
                lobbyRepository.incrementCurrentTurnIndex(lobbyId)
            } catch (e: Exception) {
                println("Erreur incrémentation : ${e.message}")
            }
        }
    }

}


