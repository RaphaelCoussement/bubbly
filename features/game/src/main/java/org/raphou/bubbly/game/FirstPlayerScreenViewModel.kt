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

    init {
        viewModelScope.launch {
            wordRepository.initializeWords()
            _words.value = wordRepository.getRandomWords(null)
        }
    }
}

