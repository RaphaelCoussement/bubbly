package org.raphou.bubbly.domain.word

import com.google.firebase.firestore.PropertyName

data class WordSelected(
    val lobbyId: String = "",
    val word: String = "",
    @PropertyName("isFound") @JvmField val isFound: Boolean = false,
    val points: Int = 0,
    val firstPlayerId: String = "",
)