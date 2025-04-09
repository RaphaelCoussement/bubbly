package org.raphou.bubbly.domain.lobby

import com.google.firebase.firestore.PropertyName

data class Lobby(
    val id: String = "",
    val code: String = "",
    val players: List<Player> = emptyList(),
    @PropertyName("isStarted") @JvmField val isStarted: Boolean = false
)


