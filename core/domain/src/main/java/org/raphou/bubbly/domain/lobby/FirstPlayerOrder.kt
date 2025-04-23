package org.raphou.bubbly.domain.lobby

data class FirstPlayerOrder(
    val lobbyId: String = "",
    val orderList: List<FirstPlayerOrderEntry> = emptyList(),
    val currentTurnIndex: Int = 0
)