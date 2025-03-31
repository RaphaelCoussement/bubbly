package org.raphou.bubbly.game

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.lobby.Player
import org.raphou.bubbly.domain.word.IWordRepository
import org.raphou.bubbly.domain.word.Word

class FirstPlayerScreenViewModel : ViewModel(), KoinComponent {
    private val wordRepository: IWordRepository by inject()

    private val _words = mutableStateOf<List<Word>>(emptyList())
    val words: State<List<Word>> get() = _words

    private val _foundWords = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val foundWords: StateFlow<Map<String, Boolean>> get() = _foundWords

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> get() = _score


    fun fetchWords(lobbyId: String) {
        viewModelScope.launch {
            wordRepository.initializeWords()
            val selectedWords = wordRepository.getRandomWords(null, lobbyId)
            _words.value = selectedWords
            checkWordStatus(lobbyId)
        }
    }

    fun checkWordStatus(lobbyId: String) {
        viewModelScope.launch {
            val statusMap = mutableMapOf<String, Boolean>()
            _words.value.forEach { word ->
                val wordInfo = wordRepository.getWordInfo(lobbyId, word.name)
                statusMap[word.name] = wordInfo?.isFound ?: false
            }
            _foundWords.value = statusMap.toMap()
        }
    }

    fun fetchFinalScore(lobbyId: String) {
        viewModelScope.launch {
            _score.value = wordRepository.getTotalPoints(lobbyId)
        }
    }

}


