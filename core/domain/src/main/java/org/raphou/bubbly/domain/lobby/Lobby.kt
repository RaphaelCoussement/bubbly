package org.raphou.bubbly.domain.lobby

import com.google.firebase.firestore.PropertyName

data class Lobby(
    val id: String = "",
    val code: String = "",
    val players: List<Player> = emptyList(),
    val firstPlayerId: String? = null,
    @PropertyName("isStarted") @JvmField val isStarted: Boolean = false,
    @PropertyName("isFirstPlayerAssigned") @JvmField val isFirstPlayerAssigned: Boolean = false,
    @PropertyName("isTimeFinished") @JvmField val isTimeFinished: Boolean = false,
    @PropertyName("isAllFirstPlayer") @JvmField val isAllFirstPlayer: Boolean = false,
    @PropertyName("firstPlayersIds") @JvmField val firstPlayersIds: List<String> = emptyList(),
    @PropertyName("isLastTurnInProgress") @JvmField val isLastTurnInProgress: Boolean = false,
    @PropertyName("currentTurn") @JvmField val currentTurn: Long = 0,
    @PropertyName("assignedTurn") @JvmField val assignedTurn: Long = -1,
    val firstPlayersPerTurn: Map<String, String>? = null,
    val themeId: String? = null,
)


