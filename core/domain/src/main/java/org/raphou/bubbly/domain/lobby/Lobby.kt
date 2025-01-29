package org.raphou.bubbly.domain.lobby

data class Lobby(
    val code: String,
    val players: List<Player>,
    val isCreator: Boolean
)