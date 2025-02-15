package org.raphou.bubbly.data.repositories

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.lobby.Lobby
import org.raphou.bubbly.domain.lobby.Player
import java.util.UUID

class LobbyRepositoryImpl : ILobbyRepository {
    private val db = Firebase.firestore
    // liste de toutes les collections ciblées dans firebase
    private val lobbiesCollection = db.collection("lobbies")
    private val playersCollection = db.collection("players")
    private val lobbyPlayersCollection = db.collection("lobby_players")

    override suspend fun createLobby(): Lobby {
        return try {
            val code = (1000..9999).random().toString()
            val lobbyId = UUID.randomUUID().toString()

            val lobby = Lobby(id = lobbyId, code = code, players = emptyList(), isCreator = true)
            lobbiesCollection.document(lobbyId).set(lobby).await()

            val existingPlayers = playersCollection.get().await()
            if (existingPlayers.isEmpty) {
                val defaultPlayers = (1..10).map {
                    Player(id = UUID.randomUUID().toString(), name = "Joueur $it")
                }
                defaultPlayers.forEach { player ->
                    playersCollection.document(player.id).set(player).await()
                }
                Log.d("LobbyRepository", "Collection 'players' créée avec succès.")
            }

            lobby
        } catch (e: Exception) {
            Log.e("LobbyRepository", "Erreur lors de la création du lobby", e)
            throw e
        }
    }

    override suspend fun joinLobby(code: String): Lobby {
        return try {
            val querySnapshot = lobbiesCollection.whereEqualTo("code", code).get().await()
            if (querySnapshot.isEmpty) {
                throw IllegalArgumentException("Lobby non trouvé")
            }

            val lobbyDoc = querySnapshot.documents.first()
            val lobby = lobbyDoc.toObject(Lobby::class.java)?.copy(id = lobbyDoc.id)
                ?: throw IllegalStateException("Lobby invalide")

            val player = Player(id = UUID.randomUUID().toString(), name = "Nouveau Joueur")
            playersCollection.document(player.id).set(player).await()

            // Associe le joueur au lobby dans la collection 'lobby_players'
            val lobbyPlayerData = mapOf(
                "lobbyId" to lobby.id,
                "playerId" to player.id
            )
            lobbyPlayersCollection.add(lobbyPlayerData).await()

            // Mise à jour la liste des joueurs dans le lobby
            val updatedPlayers = lobby.players + player
            val updatedLobby = lobby.copy(players = updatedPlayers)
            lobbiesCollection.document(lobby.id).update("players", updatedPlayers).await()

            updatedLobby
        } catch (e: Exception) {
            Log.e("LobbyRepository", "Erreur lors de la jonction au lobby", e)
            throw e
        }
    }

    override fun listenToLobbyPlayers(lobbyId: String, onUpdate: (List<Player>) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                lobbyPlayersCollection
                    .whereEqualTo("lobbyId", lobbyId)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w("LobbyRepository", "erreur des joueurs", e)
                            return@addSnapshotListener
                        }

                        GlobalScope.launch(Dispatchers.IO) {
                            val players = snapshot?.documents?.mapNotNull { document ->
                                val playerId = document.getString("playerId")
                                playerId?.let {
                                    val playerSnapshot = playersCollection.document(it).get().await()
                                    playerSnapshot.toObject(Player::class.java)
                                }
                            }?.filterIsInstance<Player>().orEmpty()

                            onUpdate(players)
                        }
                    }
            } catch (e: Exception) {
                Log.e("LobbyRepository", "Erreur lors de l'écoute des joueurs", e)
            }
        }
    }

    override suspend fun addPlayerToLobby(lobbyId: String, player: Player) {
        try {
            playersCollection.document(player.id).set(player).await()
            lobbyPlayersCollection.add(mapOf("lobbyId" to lobbyId, "playerId" to player.id)).await()
        } catch (e: Exception) {
            Log.e("LobbyRepository", "Erreur lors de l'ajout du joueur", e)
        }
    }

    override suspend fun setFirstPlayer(lobbyId: String, playerId: String) {
        try {
            lobbiesCollection.document(lobbyId).update("firstPlayerId", playerId).await()
        } catch (e: Exception) {
            Log.e("LobbyRepository", "Erreur lors de la définition du premier joueur", e)
        }
    }
}

