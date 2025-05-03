package org.raphou.bubbly.data.repositories

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.raphou.bubbly.domain.exceptions.LobbyException
import org.raphou.bubbly.domain.lobby.FirstPlayerOrder
import org.raphou.bubbly.domain.lobby.FirstPlayerOrderEntry
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
    private val firstPlayerOrdersCollection = db.collection("first_player_orders")
    private val votesCollection = db.collection("votes")

    private var playersListenerRegistration: ListenerRegistration? = null

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

    override suspend fun createLobby(themeId: String?): Lobby {
        return try {
            var code: String
            do {
                code = (1000..9999).random().toString()
                val existingLobbies = lobbiesCollection.whereEqualTo("code", code).get().await()
            } while (!existingLobbies.isEmpty) // Vérifie que le code n'est pas déjà utilisé

            val lobbyId = UUID.randomUUID().toString()
            val finalThemeId = themeId ?: "default"
            val lobby = Lobby(id = lobbyId, code = code, players = emptyList(), firstPlayerId = "", isStarted = false, isFirstPlayerAssigned = false, isTimeFinished = false, isAllFirstPlayer = false, firstPlayersIds = emptyList(), isLastTurnInProgress = false, themeId = finalThemeId)
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

            // Vérification si le joueur est déjà dans le lobby
            val existingPlayerQuerySnapshot = lobbyPlayersCollection
                .whereEqualTo("lobbyId", lobby.id)
                .whereEqualTo("playerId", player.id)
                .get()
                .await()

            if (!existingPlayerQuerySnapshot.isEmpty) {
                // Si une entrée existe déjà, on ne fait rien
                return lobby
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
        playersListenerRegistration = lobbyPlayersCollection
            .whereEqualTo("lobbyId", lobbyId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("LobbyRepository", "Erreur lors de l'écoute des joueurs", e)
                    return@addSnapshotListener
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
                        onUpdate(players)
                    } catch (playerException: Exception) {
                        Log.e("LobbyRepository", "Erreur lors de la récupération des joueurs", playerException)
                    }
                }
            }
    }

    override fun stopListeningToLobbyPlayers() {
        playersListenerRegistration?.remove()
        playersListenerRegistration = null
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

    override suspend fun assignFirstPlayerIfNeeded(lobbyId: String, currentPlayerName: String, turn: Int): String {
        val orderDoc = firstPlayerOrdersCollection.document(lobbyId).get().await()
        if (!orderDoc.exists()) throw Exception("No first player order found for this lobby")

        val orderedPlayerIds = orderDoc.get("orderedPlayerIds") as? List<String>
            ?: throw Exception("Invalid ordered player list")

        return suspendCoroutine { continuation ->
            db.runTransaction { transaction ->
                val lobbyRef = lobbiesCollection.document(lobbyId)
                val lobbySnap = transaction.get(lobbyRef)
                val lobby = lobbySnap.toObject(Lobby::class.java) ?: throw Exception("Lobby not found")

                val firstPlayersPerTurn = lobby.firstPlayersPerTurn ?: emptyMap()
                val playersCount = orderedPlayerIds.size
                val turnKey = turn.toString()

                // Si tous les joueurs ont été firstPlayer et le dernier tour a été joué
                if (firstPlayersPerTurn.size == playersCount && lobby.isLastTurnInProgress) {
                    return@runTransaction "final ranking"
                }

                // Si tous les joueurs ont été firstPlayer mais le dernier tour pas encore lancé
                if (firstPlayersPerTurn.size == playersCount && !lobby.isLastTurnInProgress) {
                    transaction.update(lobbyRef, "isLastTurnInProgress", true)
                    return@runTransaction currentPlayerName
                }

                // Si déjà défini pour ce tour
                if (firstPlayersPerTurn.containsKey(turnKey)) {
                    val existingId = firstPlayersPerTurn[turnKey]!!
                    val existingSnap = transaction.get(playersCollection.document(existingId))
                    val existingPlayer = existingSnap.toObject(Player::class.java)
                        ?: throw Exception("Assigned player not found")
                    return@runTransaction existingPlayer.name
                }

                // Sinon on assigne le joueur à l’index correspondant
                val nextIndex = firstPlayersPerTurn.size
                val selectedPlayerId = orderedPlayerIds.getOrNull(nextIndex)
                    ?: return@runTransaction "final ranking"

                // Reset tous les isFirstPlayer
                orderedPlayerIds.forEach { id ->
                    transaction.update(playersCollection.document(id), "isFirstPlayer", false)
                }

                // Assigne le nouveau joueur
                transaction.update(playersCollection.document(selectedPlayerId), "isFirstPlayer", true)

                // Met à jour la map
                val updatedMap = HashMap(firstPlayersPerTurn)
                updatedMap[turnKey] = selectedPlayerId
                transaction.update(lobbyRef, "firstPlayersPerTurn", updatedMap)

                val selectedSnap = transaction.get(playersCollection.document(selectedPlayerId))
                val selectedPlayer = selectedSnap.toObject(Player::class.java)
                    ?: throw Exception("Selected player not found")
                return@runTransaction selectedPlayer.name
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

    override suspend fun endCurrentTurn(lobbyId: String) {
        val lobbyRef = lobbiesCollection.document(lobbyId)
        db.runTransaction { transaction ->
            val snap = transaction.get(lobbyRef)
            val lobby = snap.toObject(Lobby::class.java) ?: return@runTransaction
            val nextTurn = (lobby.currentTurn) + 1

            transaction.update(lobbyRef, mapOf(
                "currentTurn" to nextTurn,
                "isFirstPlayerAssigned" to false,
                "firstPlayerId" to null,
                "assignedTurn" to -1
            ))
        }.await()
    }

    override suspend fun orderFirstPlayers(lobbyId: String, players: List<Player>) {
        val shuffled = players.shuffled()

        val orderList = shuffled.mapIndexed { index, player ->
            FirstPlayerOrderEntry(playerId = player.id, order = index)
        }

        val firstPlayerOrder = FirstPlayerOrder(
            lobbyId = lobbyId,
            orderList = orderList,
            currentTurnIndex = 0
        )

        firstPlayerOrdersCollection.document(lobbyId).set(firstPlayerOrder)
    }

    override suspend fun getPlayerIdByOrder(lobbyId: String): String? {
        val snapshot = firstPlayerOrdersCollection.document(lobbyId).get().await()
        val data = snapshot.toObject(FirstPlayerOrder::class.java)
        val currentTurnIndex = data?.currentTurnIndex ?: 0

        // Trouver le joueur correspondant au tour actuel
        val playerId = data?.orderList?.firstOrNull { it.order == currentTurnIndex }?.playerId
        return playerId
    }

    override suspend fun incrementCurrentTurnIndex(lobbyId: String) {
        val snapshot = firstPlayerOrdersCollection.document(lobbyId).get().await()
        val data = snapshot.toObject(FirstPlayerOrder::class.java)

        val currentTurnIndex = data?.currentTurnIndex ?: 0
        val newTurnIndex = currentTurnIndex + 1
        firstPlayerOrdersCollection.document(lobbyId).update("currentTurnIndex", newTurnIndex)
    }
    override suspend fun voteForPlayer(lobbyId: String, voterId: String, votedPlayerId: String) {
        val lobbyVotesDoc = votesCollection.document(lobbyId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(lobbyVotesDoc)

            if (!snapshot.exists()) {
                // Initialise le document avec le premier vote + lobbyId
                val initialData = hashMapOf(
                    "lobbyId" to lobbyId,
                    votedPlayerId to 1,
                    "totalVotes" to 1
                )
                transaction.set(lobbyVotesDoc, initialData)
            } else {
                // Met à jour les votes pour le player voté + totalVotes
                val currentVotes = (snapshot.get(votedPlayerId) as? Long)?.toInt() ?: 0
                transaction.update(lobbyVotesDoc, votedPlayerId, currentVotes + 1)

                val currentTotalVotes = (snapshot.get("totalVotes") as? Long)?.toInt() ?: 0
                transaction.update(lobbyVotesDoc, "totalVotes", currentTotalVotes + 1)
            }
        }
    }

    override fun listenToVotes(lobbyId: String, onUpdate: (Map<String, Int>, Boolean) -> Unit) {
        val lobbyVotesDoc = votesCollection.document(lobbyId)
        val playersCollection = lobbyPlayersCollection
            .whereEqualTo("lobbyId", lobbyId)

        lobbyVotesDoc.addSnapshotListener { voteSnapshot, voteError ->
            if (voteError != null || voteSnapshot == null || !voteSnapshot.exists()) return@addSnapshotListener

            val votes = voteSnapshot.data
                ?.filterKeys { it != "totalVotes" && it != "lobbyId" }
                ?.mapValues { (it.value as Long).toInt() }
                ?: emptyMap()

            // Récupération du total de votes enregistrés
            val totalVotes = (voteSnapshot.get("totalVotes") as? Long)?.toInt() ?: 0

            playersCollection.get()
                .addOnSuccessListener { playersSnapshot ->
                    val playersCount = playersSnapshot.size()
                    // Vérifie si tous les joueurs ont voté
                    val allVotesReceived = totalVotes == playersCount
                    Log.d("Votes", "totalVotes: $totalVotes / playersCount: $playersCount → allVotesReceived: $allVotesReceived")

                    // Callback avec les votes et l'état de totalVotes
                    onUpdate(votes, allVotesReceived)
                }
        }
    }
    override suspend fun addPointsToWinners(lobbyId: String, winners: List<String>) {
        val lobbyPlayersRef = lobbyPlayersCollection
            .whereEqualTo("lobbyId", lobbyId)
            .get()
            .await()

        val batch = db.batch()

        for (doc in lobbyPlayersRef.documents) {
            val playerId = doc.getString("playerId") ?: continue

            if (playerId in winners) {
                val playerDoc = playersCollection.document(playerId).get().await()
                val currentPoints = (playerDoc.getLong("points") ?: 0).toInt()

                val playerRef = playersCollection.document(playerId)
                batch.update(playerRef, "points", currentPoints + 2)
            }
        }
        batch.commit().await()
    }

    override suspend fun resetVotes(lobbyId: String) {
        votesCollection.document(lobbyId).delete().await()
    }

    override suspend fun clearPlayersVotes(lobbyId: String?) {
        if (lobbyId.isNullOrBlank()) return
        val collection = db.collection("players_votes")
        val querySnapshot = collection.whereEqualTo("lobbyId", lobbyId).get().await()
        for (document in querySnapshot.documents) {
            document.reference.delete().await()
        }
    }

    override suspend fun clearVotes(lobbyId: String?) {
        if (lobbyId.isNullOrBlank()) return
        val collection = db.collection("votes")
        val querySnapshot = collection.whereEqualTo("lobbyId", lobbyId).get().await()
        for (document in querySnapshot.documents) {
            document.reference.delete().await()
        }
    }

    override suspend fun clearLobbyPlayers(lobbyId: String?) {
        if (lobbyId.isNullOrBlank()) return
        val collection = db.collection("lobby_players")
        val querySnapshot = collection.whereEqualTo("lobbyId", lobbyId).get().await()
        for (document in querySnapshot.documents) {
            document.reference.delete().await()
        }
    }

    override suspend fun deleteLobby(lobbyId: String?) {
        if (lobbyId.isNullOrBlank()) return
        val document = db.collection("lobbies").document(lobbyId)
        document.delete().await()
    }

    override suspend fun deleteFirstPlayerOrder(lobbyId: String?) {
        if (lobbyId.isNullOrBlank()) return
        val collection = db.collection("first_player_orders")
        val querySnapshot = collection.whereEqualTo("lobbyId", lobbyId).get().await()
        for (document in querySnapshot.documents) {
            document.reference.delete().await()
        }
    }

    override suspend fun resetPlayersPoints(lobbyId: String?) {
        if (lobbyId.isNullOrBlank()) return
        val lobbyPlayersCollection = db.collection("lobby_players")
        val playersCollection = db.collection("players")

        val querySnapshot = lobbyPlayersCollection
            .whereEqualTo("lobbyId", lobbyId)
            .get()
            .await()

        for (document in querySnapshot.documents) {
            val playerId = document.getString("playerId")
            if (playerId != null) {
                val playerDoc = playersCollection.document(playerId)
                playerDoc.update("points", 0).await()
            }
        }
    }

    override suspend fun getLastFirstPlayerId(lobbyId: String): String? {
        val snapshot = db.collection("first_player_orders")
            .whereEqualTo("lobbyId", lobbyId)
            .get()
            .await()

        if (snapshot.isEmpty) return null

        val document = snapshot.documents.first()

        val orderList = document.get("orderList") as? List<Map<String, Any>> ?: return null

        val lastPlayer = orderList.maxByOrNull { it["order"] as? Long ?: 0L }

        return lastPlayer?.get("playerId") as? String
    }



}

