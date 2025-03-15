package org.raphou.bubbly.domain.word

data class Word(
    val id: String = "",
    val name: String = "",
    val theme: String = "",
    val difficulty: Difficulty = Difficulty.FACILE
)