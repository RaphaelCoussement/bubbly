package org.raphou.bubbly.data.repositories

import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.lobby.Lobby
import org.raphou.bubbly.domain.lobby.Player

class LobbyRepositoryImpl : ILobbyRepository {
    private val lobbies = mutableMapOf<String, Lobby>()

    override suspend fun createLobby(): Lobby {
        val code = (1000..9999).random().toString() // Génère un code aléatoire

        val emptyPlayers = emptyList<Player>()
        val lobby = Lobby(code = code, players = emptyPlayers, isCreator = true)
        lobbies[code] = lobby
        return lobby
    }

    override suspend fun joinLobby(code: String): Lobby {
        val lobby = lobbies[code] ?: throw IllegalArgumentException("Session non trouvée")
        val updatedPlayers = lobby.players + Player(id = "1", name = "Participant 1")
        val updatedLobby = lobby.copy(players = updatedPlayers)
        lobbies[code] = updatedLobby
        return updatedLobby
    }
}