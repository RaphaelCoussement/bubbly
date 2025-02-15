package org.raphou.bubbly.domain.lobby

interface ILobbyRepository {
    suspend fun createLobby(): Lobby
    suspend fun joinLobby(code: String): Lobby
    fun listenToLobbyPlayers(lobbyId: String, onUpdate: (List<Player>) -> Unit)
    suspend fun addPlayerToLobby(lobbyId: String, player: Player)
    suspend fun setFirstPlayer(lobbyId: String, playerId: String)
}
