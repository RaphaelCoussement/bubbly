package org.raphou.bubbly.domain.word

interface IWordRepository {
    suspend fun initializeWords()
    suspend fun getRandomWords(theme: String?): List<Word>
}