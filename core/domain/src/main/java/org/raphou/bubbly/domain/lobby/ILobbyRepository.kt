package org.raphou.bubbly.domain.lobby

interface ILobbyRepository {
    suspend fun createLobby(): Lobby
    suspend fun joinLobby(code: String, pseudo: String): Lobby
    fun listenToLobbyPlayers(lobbyId: String, onUpdate: (List<Player>) -> Unit)
    suspend fun addPlayerToLobby(lobbyId: String, player: Player)
    suspend fun setFirstPlayer(lobbyId: String, playerId: String)
    suspend fun startLobby(lobbyId: String)
    suspend fun getLobby(lobbyId: String): Lobby
    suspend fun getLobbyByCode(code: String): Lobby
    fun listenToLobbyUpdates(lobbyId: String, onUpdate: (Lobby) -> Unit)
    suspend fun addPlayer(pseudo: String) : Player
    suspend fun isPseudoExisting(pseudo: String) : Boolean
    suspend fun getPlayer(pseudo: String) : Player
    suspend fun assignFirstPlayerIfNeeded(lobbyId: String, currentPlayerName: String): String
    suspend fun getPlayersRanking(lobbyId: String): List<Pair<String, Int>>
    suspend fun deletePlayer(pseudo: String)
    suspend fun setIsTimeFinished(lobbyId: String)
    suspend fun isTimeFinished(lobbyId: String) : Boolean
    suspend fun resetLobby(lobbyId: String)
}
