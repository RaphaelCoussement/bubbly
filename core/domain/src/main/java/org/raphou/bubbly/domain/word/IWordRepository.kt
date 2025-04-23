package org.raphou.bubbly.domain.word

interface IWordRepository {
    suspend fun initializeWords()
    suspend fun getRandomWords(theme: String?, lobbyId: String): List<Word>
    suspend fun addPlayerSuggestion(lobbyId: String, playerId: String, word: String)
    suspend fun addWordSelected(lobbyId: String, word: String, isFound: Boolean, points: Int, playerId: String)
    suspend fun getWordInfo(lobbyId: String,playerId: String, word: String): WordSelected?
    suspend fun checkGameStatus(lobbyId: String): Boolean
    suspend fun calculatePlayerScore(lobbyId: String, playerId: String): Int
    suspend fun resetGame(lobbyId: String, playerId: String)
    suspend fun getWordsToFind(lobbyId: String): List<String>
    suspend fun getTotalPoints(lobbyId: String, playerId: String): Int
    suspend fun getTotalPlayerPoints(lobbyId: String, playerId: String): Int
    suspend fun deletePlayerWordSuggestion(pseudo: String)
    suspend fun clearPlayerSuggestions(lobbyId: String)
    suspend fun clearSelectedWords(lobbyId: String)
}

