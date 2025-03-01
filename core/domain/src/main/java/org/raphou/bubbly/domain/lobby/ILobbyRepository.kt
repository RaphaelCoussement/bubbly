package org.raphou.bubbly.domain.lobby

interface ILobbyRepository {
    suspend fun createLobby(): Lobby
    suspend fun joinLobby(code: String): Lobby
    fun listenToLobbyPlayers(lobbyId: String, onUpdate: (List<Player>) -> Unit)
    suspend fun addPlayerToLobby(lobbyId: String, player: Player)
    suspend fun setFirstPlayer(lobbyId: String, playerId: String)
    suspend fun startLobby(lobbyId: String)
    suspend fun getLobby(lobbyId: String): Lobby
    suspend fun getLobbyByCode(code: String): Lobby

    fun listenToLobbyUpdates(lobbyId: String, onUpdate: (Lobby) -> Unit)
}
