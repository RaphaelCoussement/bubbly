package org.raphou.bubbly.domain.lobby

data class Lobby(
    val id: String = "",
    val code: String = "",
    val players: List<Player> = emptyList(),
    val isCreator: Boolean = false
)
