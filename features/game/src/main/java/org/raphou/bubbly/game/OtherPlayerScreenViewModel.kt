import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.raphou.bubbly.domain.word.IWordRepository
import org.raphou.bubbly.domain.word.Word
import org.raphou.bubbly.domain.word.WordSelected
import java.lang.Exception

class OtherPlayerScreenViewModel : ViewModel(), KoinComponent {

    private val wordRepository: IWordRepository by inject()

    private val _suggestions = mutableStateOf<List<String>>(emptyList())
    val suggestions: State<List<String>> = _suggestions

    private val _score = mutableStateOf(0)
    val score: State<Int> = _score

    private val _remainingSuggestions = mutableStateOf(10)
    val remainingSuggestions: State<Int> = _remainingSuggestions

    private val _isTimeUp = mutableStateOf(false)
    val isTimeUp: State<Boolean> = _isTimeUp

    private val _wordsToFind = mutableStateOf<List<String>>(emptyList())
    val wordsToFind: State<List<String>> = _wordsToFind

    fun addPlayerSuggestion(lobbyId: String, playerId: String, suggestedWord: String) {
        viewModelScope.launch {
            try {
                if (_remainingSuggestions.value > 0) {
                    Log.d("OtherPlayerScreenViewModel", "Adding word: $suggestedWord")
                    wordRepository.addPlayerSuggestion(lobbyId, playerId, suggestedWord)
                    _suggestions.value = _suggestions.value + suggestedWord
                    _remainingSuggestions.value -= 1
                    Log.d("OtherPlayerScreenViewModel", "Remaining suggestions: ${_remainingSuggestions.value}")
                } else {
                    Log.d("OtherPlayerScreenViewModel", "No remaining suggestions.")
                    throw Exception("Nombre de suggestions atteint")
                }
            } catch (e: Exception) {
                Log.e("OtherPlayerScreenViewModel", "Error adding suggestion: ${e.message}")
            }
        }
    }

    fun setTimeUp() {
        _isTimeUp.value = true
    }

    fun resetGame(lobbyId: String, playerId: String) {
        viewModelScope.launch {
            try {
                wordRepository.resetGame(lobbyId, playerId)
                _suggestions.value = emptyList()
                val finalScore = wordRepository.calculatePlayerScore(lobbyId, playerId)
                _score.value = finalScore
                val wordsToFindList = wordRepository.getWordsToFind(lobbyId)
                _wordsToFind.value = wordsToFindList
                _isTimeUp.value = true
            } catch (e: Exception) {
                Log.e("OtherPlayerScreenViewModel", "Error resetting game: ${e.message}")
            }
        }
    }
}
