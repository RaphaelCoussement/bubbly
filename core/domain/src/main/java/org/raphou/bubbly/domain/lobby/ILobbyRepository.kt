package org.raphou.bubbly.domain.lobby

interface ILobbyRepository {
    suspend fun createLobby(): Lobby
    suspend fun joinLobby(code: String): Lobby
}