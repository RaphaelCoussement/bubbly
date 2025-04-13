package org.raphou.bubbly.data.repositories

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LobbyRepositoryImpl : ILobbyRepository {
    private val db = Firebase.firestore
    // liste de toutes les collections ciblées dans firebase
    private val lobbiesCollection = db.collection("lobbies")
    private val playersCollection = db.collection("players")
    private val lobbyPlayersCollection = db.collection("lobby_players")


    override suspend fun setIsTimeFinished(lobbyId: String) {
        val lobbyRef = lobbiesCollection.document(lobbyId)
        lobbyRef.update("isTimeFinished", true)
    }

    override suspend fun isTimeFinished(lobbyId: String): Boolean {
        val lobbyRef = lobbiesCollection.document(lobbyId)
        val docSnapshot = lobbyRef.get().await()
        return docSnapshot.getBoolean("isTimeFinished") ?: false
    }

    override suspend fun deletePlayer(pseudo: String) {
        // Recherche le joueur par son pseudo
        val querySnapshot = playersCollection.whereEqualTo("name", pseudo).get().await()

        if (querySnapshot.isEmpty) {
            throw Exception("Aucun joueur trouvé avec ce pseudo")
        }

        // Récupérer le premier joueur correspondant
        val playerDocument = querySnapshot.documents.first()
        val playerId = playerDocument.id

        // Supprimer le joueur de la collection players
        playersCollection.document(playerId).delete().await()

        // Supprimer ce joueur de tous les lobbies où il est inscrit
        val lobbyPlayersQuerySnapshot = lobbyPlayersCollection.whereEqualTo("playerId", playerId).get().await()
        lobbyPlayersQuerySnapshot.forEach { lobbyPlayer ->
            // Supprimer le lien entre le joueur et le lobby
            lobbyPlayersCollection.document(lobbyPlayer.id).delete().await()
        }
    }

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
            val lobby = Lobby(id = lobbyId, code = code, players = emptyList(), firstPlayerId = "", isStarted = false, isFirstPlayerAssigned = false, isTimeFinished = false, isAllFirstPlayer = false, firstPlayersIds = emptyList(), isLastTurnInProgress = false)
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

    override suspend fun assignFirstPlayerIfNeeded(lobbyId: String, currentPlayerName: String): String {
        val lobbyRef = lobbiesCollection.document(lobbyId)
        val lobbySnap = lobbyRef.get().await()
        val lobby = lobbySnap.toObject(Lobby::class.java) ?: throw Exception("Lobby not found")

        // Vérifier si tous les joueurs ont été désignés comme premier joueur
        val lobbyPlayersSnapshot = lobbyPlayersCollection
            .whereEqualTo("lobbyId", lobbyId)
            .get()
            .await()

        val playersCount = lobbyPlayersSnapshot.size()
        val playersThatHaveBeenFirstPlayer = lobby.firstPlayersIds.size

        if (playersThatHaveBeenFirstPlayer == playersCount) {
            if (!lobby.isLastTurnInProgress) {
                // On commence le dernier tour pour le dernier joueur
                lobbyRef.update("isLastTurnInProgress", true).await()
                return currentPlayerName // on laisse le joueur courant jouer son dernier tour
            } else {
                // Dernier tour déjà fait, on termine
                return "final ranking"
            }
        }

        // Récupérer les joueurs dans la collection des joueurs
        val playerIds = lobbyPlayersSnapshot.documents.mapNotNull { it.getString("playerId") }

        if (playerIds.isEmpty()) throw Exception("No players found for this lobby")

        // Vérification AVANT la transaction
        val allPlayers = playerIds.mapNotNull { id ->
            val snap = playersCollection.document(id).get().await()
            snap.toObject(Player::class.java)?.copy(id = id)
        }

        // Si tous les joueurs ont été premier joueur, on peut finir la partie
        val allHaveBeenFirstPlayer = allPlayers.all { it.isFirstPlayer }

        if (allHaveBeenFirstPlayer) {
            // Marquer le lobby comme terminé
            lobbyRef.update("isAllFirstPlayer", true).await()
            return "final ranking"
        }

        // Si la partie n'est pas terminée, procéder à l'assignation d'un nouveau premier joueur
        return suspendCoroutine { continuation ->
            db.runTransaction { transaction ->
                val lobbySnap = transaction.get(lobbyRef)
                val lobby = lobbySnap.toObject(Lobby::class.java) ?: throw Exception("Lobby not found")

                // Si un premier joueur est déjà assigné, on renvoie le nom du joueur actuel
                if (lobby.isFirstPlayerAssigned) {
                    val playerSnap = transaction.get(playersCollection.document(lobby.firstPlayerId))
                    val player = playerSnap.toObject(Player::class.java) ?: throw Exception("Player not found")
                    return@runTransaction player.name
                }

                // Récupérer tous les joueurs
                val players = playerIds.mapNotNull { id ->
                    val snap = transaction.get(playersCollection.document(id))
                    snap.toObject(Player::class.java)?.copy(id = id)
                }

                // Filtrer les joueurs qui ne sont pas encore passés premiers joueurs
                val availablePlayers = players.filter { !it.isFirstPlayer && !lobby.firstPlayersIds.contains(it.id) }

                if (availablePlayers.isEmpty()) {
                    // Si tous les joueurs ont été assignés, finir la partie
                    return@runTransaction "final ranking" // ce cas ne devrait pas arriver normalement
                }

                // Sélectionner un joueur aléatoire parmi les joueurs disponibles
                val selected = availablePlayers.random()

                // Ajouter le joueur sélectionné à la liste des joueurs passés premiers
                transaction.update(lobbyRef, mapOf(
                    "firstPlayerId" to selected.id,
                    "isFirstPlayerAssigned" to true,
                    "firstPlayersIds" to FieldValue.arrayUnion(selected.id)
                ))

                // Mettre à jour le joueur sélectionné comme premier joueur
                transaction.update(playersCollection.document(selected.id), mapOf(
                    "isFirstPlayer" to true
                ))

                return@runTransaction selected.name
            }.addOnSuccessListener { result ->
                continuation.resume(result)
            }.addOnFailureListener { exception ->
                continuation.resumeWith(Result.failure(exception))
            }
        }
    }

    override suspend fun getPlayersRanking(lobbyId: String): List<Pair<String, Int>> {
        val lobbyPlayersSnapshot = lobbyPlayersCollection
            .whereEqualTo("lobbyId", lobbyId)
            .get()
            .await()

        val playerIds = lobbyPlayersSnapshot.documents.mapNotNull { it.getString("playerId") }

        val playersSnapshot = playersCollection
            .whereIn(FieldPath.documentId(), playerIds)
            .get()
            .await()

        return playersSnapshot.documents.mapNotNull { doc ->
            val name = doc.getString("name")
            val points = doc.getLong("points")?.toInt()
            if (name != null && points != null) name to points else null
        }.sortedByDescending { it.second }
    }

    override suspend fun resetLobby(lobbyId: String) {
        val lobbyRef = db.collection("lobbies").document(lobbyId)

        lobbyRef.update(
            mapOf(
                "isFirstPlayerAssigned" to false,
                "isStarted" to false,
                "isTimeFinished" to false
            )
        ).await()
    }

}

