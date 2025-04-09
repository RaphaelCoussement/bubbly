package org.raphou.bubbly.data.repositories

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.raphou.bubbly.domain.exceptions.LobbyException
import org.raphou.bubbly.domain.home.IUserPreferencesRepository
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

    override suspend fun getPlayer(pseudo: String): Player {
        // Recherche le joueur par son pseudo
        val querySnapshot = playersCollection.whereEqualTo("name", pseudo).get().await()

        // Vérifier si un joueur avec ce pseudo existe
        if (querySnapshot.isEmpty) {
            throw Exception("Aucun joueur trouvé avec ce pseudo")
        }

        // Récupérer le premier joueur correspondant
        val playerDocument = querySnapshot.documents.first()
        return playerDocument.toObject(Player::class.java) ?: throw Exception("Erreur de conversion du joueur")
    }

    override suspend fun addPlayer(pseudo: String): Player {
        // Vérifier si le pseudo existe déjà
        if (isPseudoExisting(pseudo)) {
            throw Exception("Le pseudo est déjà pris")
        }

        val player = Player(
            id = UUID.randomUUID().toString(), // Générer un ID unique pour le joueur
            name = pseudo
        )

        // Ajoute le joueur à la collection "players"
        playersCollection.document(player.id).set(player)

        return player
    }

    override suspend fun isPseudoExisting(pseudo: String): Boolean {
        return try {
            val querySnapshot = playersCollection
                .whereEqualTo("name", pseudo)
                .get()
                .await()

            querySnapshot.isEmpty.not()
        } catch (e: Exception) {
            Log.e("FirestoreError", "Erreur lors de la vérification du pseudo: ${e.message}")
            false
        }
    }


    override suspend fun getLobby(lobbyId: String): Lobby {
        return try {
            val document = lobbiesCollection.document(lobbyId).get().await()
            document.toObject(Lobby::class.java)?.copy(id = document.id)
                ?: throw LobbyException.LobbyNotFound("Lobby avec l'ID $lobbyId introuvable.")
        } catch (e: Exception) {
            Log.e("LobbyRepository", "Erreur lors de la récupération du lobby", e)
            throw e
        }
    }

    override suspend fun createLobby(): Lobby {
        return try {
            var code: String
            do {
                code = (1000..9999).random().toString()
                val existingLobbies = lobbiesCollection.whereEqualTo("code", code).get().await()
            } while (!existingLobbies.isEmpty) // Vérifie que le code n'est pas déjà utilisé

            val lobbyId = UUID.randomUUID().toString()
            val lobby = Lobby(id = lobbyId, code = code, players = emptyList(), isStarted = false)
            lobbiesCollection.document(lobbyId).set(lobby).await()

            lobby
        } catch (e: Exception) {
            Log.e("LobbyRepository", "Erreur lors de la création du lobby", e)
            throw LobbyException.LobbyCreationFailed("Impossible de créer le lobby", e)
        }
    }

    override suspend fun joinLobby(code: String, pseudo : String): Lobby {
        return try {
            // Recherche du lobby avec le code
            val querySnapshot = lobbiesCollection.whereEqualTo("code", code).get().await()
            if (querySnapshot.isEmpty) {
                throw LobbyException.LobbyNotFound("Aucun lobby trouvé avec le code $code")
            }

            // Récupération du lobby
            val lobbyDoc = querySnapshot.documents.first()
            val lobby = lobbyDoc.toObject(Lobby::class.java)?.copy(id = lobbyDoc.id)
                ?: throw LobbyException.InvalidLobby("Lobby avec le code $code est invalide.")

            // Récupère le pseudo enregistré sur le téléphone
            val pseudo = pseudo

            // Récupérer le player correspondant au pseudo dans Firebase
            val playerQuerySnapshot = playersCollection.whereEqualTo("name", pseudo).get().await()
            val player = if (playerQuerySnapshot != null && playerQuerySnapshot.documents.isNotEmpty()) {
                val playerDoc = playerQuerySnapshot.documents.first()
                playerDoc.toObject(Player::class.java)?.copy(id = playerDoc.id)
                    ?: throw LobbyException.InvalidPlayer("Le joueur avec le pseudo $pseudo est invalide.")
            } else {
                throw LobbyException.PlayerNotFound("Aucun joueur trouvé avec le pseudo $pseudo")
            }

            // Associer le joueur au lobby dans la collection 'lobby_players'
            try {
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
                throw LobbyException.PlayerJoinFailed("Impossible d'ajouter le joueur au lobby $code", e)
            }
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
                            Log.w("LobbyRepository", "Erreur lors de l'écoute des joueurs", e)
                            throw LobbyException.LobbyNotFound("Lobby avec l'id $lobbyId non trouvé")
                        }

                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                val players = snapshot?.documents?.mapNotNull { document ->
                                    val playerId = document.getString("playerId")
                                    playerId?.let {
                                        val playerSnapshot = playersCollection.document(it).get().await()
                                        playerSnapshot.toObject(Player::class.java)
                                    }
                                }?.filterIsInstance<Player>().orEmpty()

                                if (players.isEmpty()) {
                                    throw LobbyException.PlayerJoinFailed("Aucun joueur trouvé pour le lobby $lobbyId")
                                }

                                onUpdate(players)
                            } catch (playerException: Exception) {
                                Log.e("LobbyRepository", "Erreur lors de la récupération des joueurs", playerException)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("LobbyRepository", "Erreur lors de l'écoute des joueurs", e)
                throw LobbyException.LobbyNotFound("Erreur lors de l'écoute des joueurs du lobby $lobbyId")
            }
        }
    }

    override suspend fun addPlayerToLobby(lobbyId: String, player: Player) {
        try {
            lobbyPlayersCollection.add(mapOf("lobbyId" to lobbyId, "playerId" to player.id)).await()

        } catch (e: FirebaseFirestoreException) {
            Log.e("LobbyRepository", "Erreur lors de l'ajout du joueur dans la base de données", e)
            throw LobbyException.PlayerJoinFailed("Erreur lors de l'ajout du joueur ${player.id} dans le lobby $lobbyId", e)
        } catch (e: Exception) {
            Log.e("LobbyRepository", "Erreur lors de l'ajout du joueur", e)
            throw LobbyException.PlayerJoinFailed("Erreur inconnue lors de l'ajout du joueur ${player.id} dans le lobby $lobbyId", e)
        }
    }

    override suspend fun setFirstPlayer(lobbyId: String, playerId: String) {
        try {
            lobbiesCollection.document(lobbyId).update("firstPlayerId", playerId).await()

        } catch (e: FirebaseFirestoreException) {
            Log.e("LobbyRepository", "Erreur lors de la définition du premier joueur dans la base de données", e)
            throw LobbyException.PlayerJoinFailed("Erreur lors de la définition du premier joueur dans le lobby $lobbyId", e)
        } catch (e: Exception) {
            Log.e("LobbyRepository", "Erreur lors de la définition du premier joueur", e)
            throw LobbyException.PlayerJoinFailed("Erreur inconnue lors de la définition du premier joueur dans le lobby $lobbyId", e)
        }
    }

    override suspend fun startLobby(lobbyId: String) {
        try {
            lobbiesCollection.document(lobbyId).update("isStarted", true).await()
            Log.d("JoinLobbyScreenVM", "Mise à jour envoyée à Firestore !")

            delay(1000)

            val updatedSession = lobbiesCollection.document(lobbyId)
                .get(Source.SERVER)
                .await()
                .toObject(Lobby::class.java)

            Log.d("JoinLobbyScreenVM", "Updated session: $updatedSession")

        } catch (e: FirebaseFirestoreException) {
            Log.e("LobbyRepository", "Erreur lors du démarrage du lobby", e)
            throw LobbyException.LobbyCreationFailed("Erreur lors du démarrage du lobby $lobbyId", e)
        } catch (e: Exception) {
            Log.e("LobbyRepository", "Erreur lors du démarrage du lobby", e)
            throw LobbyException.LobbyCreationFailed("Erreur inconnue lors du démarrage du lobby $lobbyId", e)
        }
    }

    override suspend fun getLobbyByCode(code: String): Lobby {
        return try {
            val snapshot = lobbiesCollection
                .whereEqualTo("code", code)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                throw LobbyException.LobbyNotFound("Lobby avec le code $code non trouvé.")
            }

            val lobbyDoc = snapshot.documents.first()
            lobbyDoc.toObject(Lobby::class.java)?.copy(id = lobbyDoc.id)
                ?: throw LobbyException.InvalidLobby("Le lobby avec le code $code est invalide.")

        } catch (e: FirebaseFirestoreException) {
            Log.e("LobbyRepository", "Erreur lors de la récupération du lobby par code", e)
            throw LobbyException.LobbyNotFound("Erreur lors de la récupération du lobby $code")
        } catch (e: IllegalArgumentException) {
            Log.e("LobbyRepository", "Lobby non trouvé", e)
            throw LobbyException.LobbyNotFound("Lobby avec le code $code non trouvé.")
        } catch (e: Exception) {
            Log.e("LobbyRepository", "Erreur lors de la récupération du lobby par code", e)
            throw LobbyException.LobbyNotFound("Erreur inconnue lors de la récupération du lobby $code")
        }
    }

    override fun listenToLobbyUpdates(lobbyId: String, onUpdate: (Lobby) -> Unit) {
        try {
            lobbiesCollection.document(lobbyId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("LobbyRepository", "Erreur lors de l'écoute du lobby", error)
                        throw LobbyException.LobbyNotFound("Erreur lors de l'écoute du lobby avec ID $lobbyId")
                    }

                    snapshot?.toObject(Lobby::class.java)?.let { lobby ->
                        onUpdate(lobby.copy(id = snapshot.id)) // Mise à jour du lobby avec son ID Firestore
                        Log.d("LobbyRepository", "🔥 Mise à jour en temps réel : $lobby")
                    } ?: run {
                        throw LobbyException.InvalidLobby("Le lobby avec ID $lobbyId est invalide ou introuvable.")
                    }
                }
        } catch (e: FirebaseFirestoreException) {
            // Erreur spécifique Firestore lors de l'ajout du SnapshotListener
            Log.e("LobbyRepository", "Erreur lors de l'écoute du lobby", e)
            throw LobbyException.LobbyNotFound("Erreur de connexion ou d'accès aux données du lobby $lobbyId")
        } catch (e: Exception) {
            // Erreur générique
            Log.e("LobbyRepository", "Erreur générale lors de l'écoute du lobby", e)
            throw LobbyException.LobbyNotFound("Erreur inconnue lors de l'écoute du lobby $lobbyId")
        }
    }

}

