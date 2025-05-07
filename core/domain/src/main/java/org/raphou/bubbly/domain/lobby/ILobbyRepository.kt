package org.raphou.bubbly.domain.lobby

interface ILobbyRepository {
    suspend fun createLobby(themeId: String?): Lobby
    suspend fun joinLobby(code: String, pseudo: String): Lobby
    fun listenToLobbyPlayers(lobbyId: String, onUpdate: (List<Player>) -> Unit)
    fun stopListeningToLobbyPlayers()
    suspend fun addPlayerToLobby(lobbyId: String, player: Player)
    suspend fun setFirstPlayer(lobbyId: String, playerId: String)
    suspend fun startLobby(lobbyId: String)
    suspend fun getLobby(lobbyId: String): Lobby
    suspend fun getLobbyByCode(code: String): Lobby
    fun listenToLobbyUpdates(lobbyId: String, onUpdate: (Lobby) -> Unit)
    suspend fun addPlayer(pseudo: String) : Player
    suspend fun isPseudoExisting(pseudo: String) : Boolean
    suspend fun getPlayer(pseudo: String) : Player
    suspend fun assignFirstPlayerIfNeeded(lobbyId: String, currentPlayer: String, turn: Int): String
    suspend fun getPlayersRanking(lobbyId: String): List<Pair<String, Int>>
    suspend fun deletePlayer(pseudo: String)
    suspend fun setIsTimeFinished(lobbyId: String)
    suspend fun isTimeFinished(lobbyId: String) : Boolean
    suspend fun resetLobby(lobbyId: String)
    suspend fun endCurrentTurn(lobbyId: String)
    suspend fun orderFirstPlayers(lobbyId: String, players: List<Player>)
    suspend fun getPlayerIdByOrder(lobbyId: String): String?
    suspend fun incrementCurrentTurnIndex(lobbyId: String)
    suspend fun voteForPlayer(lobbyId: String, voterId: String, votedPlayerId: String)
    fun listenToVotes(lobbyId: String, onUpdate: (Map<String, Int>, Boolean) -> Unit)
    suspend fun addPointsToWinners(lobbyId: String, winners: List<String>)
    suspend fun getLastFirstPlayerId(lobbyId: String): String?
    suspend fun resetVotes(lobbyId: String)
    suspend fun clearPlayersVotes(lobbyId: String?)
    suspend fun clearVotes(lobbyId: String?)
    suspend fun clearLobbyPlayers(lobbyId: String?)
    suspend fun deleteLobby(lobbyId: String?)
    suspend fun deleteFirstPlayerOrder(lobbyId: String?)
    suspend fun resetPlayersPoints(lobbyId: String?)
    suspend fun isTimeStarted(lobbyId: String)
    suspend fun isTimerStarted(lobbyId: String): Boolean
}
