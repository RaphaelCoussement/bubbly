package org.raphou.bubbly.domain.lobby

import com.google.firebase.firestore.PropertyName

data class Player(
    val id: String = "",
    val name: String = "",
    @PropertyName("isFirstPlayer") @JvmField val isFirstPlayer: Boolean = false,
    val points: Int = 0
)