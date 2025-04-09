package org.raphou.bubbly.domain.exceptions

sealed class LobbyException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class LobbyNotFound(message: String) : LobbyException(message)
    class PlayerNotFound(message: String) : LobbyException(message)
    class InvalidPlayer(message: String) : LobbyException(message)
    class LobbyCreationFailed(message: String, cause: Throwable? = null) : LobbyException(message, cause)
    class InvalidLobby(message: String) : LobbyException(message)
    class PlayerJoinFailed(message: String, cause: Throwable? = null) : LobbyException(message, cause)
    class PlayerAlreadyInLobby(message: String, cause: Throwable? = null) : LobbyException(message, cause)
}
