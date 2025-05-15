package org.raphou.bubbly.data.repositories

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.raphou.bubbly.domain.exceptions.WordException
import org.raphou.bubbly.domain.word.Difficulty
import org.raphou.bubbly.domain.word.IWordRepository
import org.raphou.bubbly.domain.word.PlayerSuggestion
import org.raphou.bubbly.domain.word.Word
import org.raphou.bubbly.domain.word.WordSelected
import java.io.InputStream
import kotlin.random.Random

class WordRepositoryImpl(private val context: Context) : IWordRepository {
    private val db = Firebase.firestore
    private val wordsCollection = db.collection("words")
    private val playerSuggestionsCollection = db.collection("player_suggestions")
    private val wordsSelectedCollection = db.collection("words_selected")
    private val playersCollection = db.collection("players")
    private val themesCollection = db.collection("themes")

    override suspend fun initializeWords() {
        try {
            val existingWords = wordsCollection.get().await()
            if (existingWords.isEmpty) {
                val jsonString = loadJsonFromAssets("words.json")

                val wordsData = try {
                    parseWordsFromJson(jsonString)
                } catch (e: Exception) {
                    Log.e("WordRepository", "Erreur lors du parsing du fichier JSON", e)
                    throw WordException.WordParsingFailed("Erreur lors du parsing du fichier JSON.", e)
                }

                try {
                    wordsData.forEach { word ->
                        val wordId = generateWordId(word) // génère un ID unique
                        val wordWithId = word.copy(id = wordId)
                        wordsCollection.document(wordWithId.id).set(wordWithId).await()
                    }
                    Log.d("WordRepository", "Collection 'words' créée avec succès.")
                } catch (e: Exception) {
                    Log.e("WordRepository", "Erreur lors de l'insertion des mots dans Firestore", e)
                    throw WordException.WordInsertionFailed("Erreur lors de l'insertion des mots dans Firestore.", e)
                }
            }
        } catch (e: Exception) {
            Log.e("WordRepository", "Erreur lors de l'initialisation des mots", e)
            throw WordException.WordInitializationFailed("Erreur lors de l'initialisation des mots.", e)
        }
    }

    private fun loadJsonFromAssets(fileName: String): String {
        val inputStream: InputStream = context.assets.open(fileName)
        return inputStream.bufferedReader().use { it.readText() }
    }

    private suspend fun parseWordsFromJson(jsonString: String): List<Word> {
        val jsonObject = JSONObject(jsonString)
        val words = mutableListOf<Word>()

        // On parcourt chaque thème dans le fichier JSON
        for (themeName in jsonObject.keys()) {
            val themeId = getThemeIdByName(themeName)

            val wordsArray = jsonObject.getJSONArray(themeName)
            for (i in 0 until wordsArray.length()) {
                val wordName = wordsArray.getString(i)
                words.add(
                    Word(
                        name = wordName,
                        themeId = themeId
                    )
                )
            }
        }

        return words
    }

    private suspend fun getThemeIdByName(themeName: String): String {
        val querySnapshot = themesCollection
            .whereEqualTo("name", themeName)
            .limit(1)
            .get()
            .await()

        if (querySnapshot.isEmpty) {
            throw WordException.WordParsingFailed("Thème '$themeName' introuvable dans Firestore.")
        }

        return querySnapshot.documents.first().id
    }

    private fun generateWordId(word: Word): String {
        return "${word.themeId}_${word.name.hashCode()}"
    }

    override suspend fun getRandomWords(themeId: String?, lobbyId: String): List<Word> {
        return try {
            // Récupérer tous les mots (filtrés par thème si nécessaire)
            val query = if (themeId != null) {
                wordsCollection.whereEqualTo("themeId", themeId)
            } else {
                wordsCollection
            }

            val result = query.get().await()
            val allWords = result.documents.mapNotNull { it.toObject(Word::class.java) }.toMutableList()

            // On mélange la liste pour tirer au hasard sans doublon
            allWords.shuffle()

            // Nombre de mots à sélectionner (1 par difficulté)
            val numberOfWords = Difficulty.values().size
            val selectedWords = mutableListOf<Word>()

            for (i in 0 until numberOfWords) {
                if (i >= allWords.size) break // Sécurité : pas assez de mots

                val word = allWords[i]
                selectedWords.add(word)

                val difficulty = Difficulty.values()[i]
                val points = when (difficulty) {
                    Difficulty.FACILE -> 1
                    Difficulty.MOYEN -> 2
                    Difficulty.DIFFICILE -> 3
                    Difficulty.EXTREME -> 4
                }

                val chosenWord = mapOf(
                    "lobbyId" to lobbyId,
                    "word" to word.name,
                    "difficulty" to difficulty.name,
                    "points" to points,
                    "isFound" to false,
                    "firstPlayerId" to ""
                )

                wordsSelectedCollection.add(chosenWord).await()
            }

            selectedWords
        } catch (e: Exception) {
            Log.e("WordRepository", "Erreur lors de la récupération des mots", e)
            throw WordException.WordRetrievalFailed("Erreur lors de la récupération des mots depuis la base de données.", e)
        }
    }

    override suspend fun addWordSelected(lobbyId: String, word: String, isFound: Boolean, points: Int, playerId: String) {
        try {
            val selectedWord = WordSelected(lobbyId, word, isFound, points, playerId)
            wordsSelectedCollection.add(selectedWord).await()
        } catch (e: Exception) {
            Log.e("WordRepository", "Erreur lors de l'ajout du mot sélectionné", e)
            throw WordException.WordInsertionFailed("Erreur lors de l'ajout du mot sélectionné.", e)
        }
    }

    override suspend fun addPlayerSuggestion(lobbyId: String, playerId: String, word: String) {
        try {
            val query = playerSuggestionsCollection
                .whereEqualTo("lobbyId", lobbyId)
                .whereEqualTo("playerId", playerId)
                .get()
                .await()

            if (query.size() >= 10) {
                throw WordException.WordLimitExceeded("Vous avez déjà fait 10 suggestions.")
            }

            val suggestion = PlayerSuggestion(lobbyId, playerId, word)
            playerSuggestionsCollection.add(suggestion).await()

            val wordQuery = wordsSelectedCollection
                .whereEqualTo("lobbyId", lobbyId)
                .get()
                .await()

            for (document in wordQuery.documents) {
                val selectedWord = document.getString("word") ?: continue
                val isAlreadyFound = document.getBoolean("isFound") ?: false

                if (isSimilar(selectedWord, word) && !isAlreadyFound) {
                    // MAJ mot comme trouvé + playerId
                    document.reference.update(
                        mapOf(
                            "isFound" to true,
                            "firstPlayerId" to playerId
                        )
                    ).await()
                    break
                }
            }

        } catch (e: Exception) {
            Log.e("WordRepository", "Erreur lors de l'ajout de la suggestion", e)
            throw WordException.WordInsertionFailed("Erreur lors de l'ajout de la suggestion.", e)
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]) + 1
                }
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun isSimilar(word1: String, word2: String): Boolean {
        val normalized1 = word1.lowercase().trim()
        val normalized2 = word2.lowercase().trim()

        if (kotlin.math.abs(normalized1.length - normalized2.length) > 2) return false

        return levenshteinDistance(normalized1, normalized2) <= 1
    }

    override suspend fun getWordInfo(lobbyId: String,playerId: String, word: String): WordSelected? {
        try {
            val wordQuery = wordsSelectedCollection
                .whereEqualTo("lobbyId", lobbyId)
                .get()
                .await()

            if (wordQuery.isEmpty) return null

            // Vérifier si le mot exact existe
            val wordDocument = wordQuery.documents.firstOrNull {
                it.getString("word") == word
            }

            if (wordDocument != null) {
                return WordSelected(
                    lobbyId = wordDocument.getString("lobbyId") ?: "",
                    word = wordDocument.getString("word") ?: "",
                    isFound = wordDocument.getBoolean("isFound") ?: false,
                    points = wordDocument.getLong("points")?.toInt() ?: 0,
                    firstPlayerId = wordDocument.getString("firstPlayerId") ?: "",
                )
            }

            // similitude
            for (doc in wordQuery.documents) {
                val dbWord = doc.getString("word") ?: continue
                if (isSimilar(dbWord, word)) {
                    return WordSelected(
                        lobbyId = doc.getString("lobbyId") ?: "",
                        word = dbWord,
                        isFound = doc.getBoolean("isFound") ?: false,
                        points = doc.getLong("points")?.toInt() ?: 0,
                        firstPlayerId = doc.getString("firstPlayerId") ?: "",
                    )
                }
            }

            return null
        } catch (e: Exception) {
            Log.e("WordRepository", "Erreur lors de la récupération des informations du mot", e)
            return null
        }
    }


    override suspend fun checkGameStatus(lobbyId: String): Boolean {
        val wordsQuery = wordsSelectedCollection.whereEqualTo("lobbyId", lobbyId).get().await()
        val allFound = wordsQuery.documents.all { it.getBoolean("isFound") == true }
        return allFound
    }

    override suspend fun calculatePlayerScore(lobbyId: String, playerId: String): Int {
        var totalScore = 0

        // Récupérer les suggestions pour le lobby
        val suggestionsQuery = playerSuggestionsCollection.whereEqualTo("lobbyId", lobbyId).get().await()

        // Récupérer le document du joueur dans la collection "players"
        val playerDocument = playersCollection.document(playerId).get().await()

        // Vérifier si le joueur existe dans la base de données
        if (!playerDocument.exists()) {
            Log.e("calculatePlayerScore", "Erreur: joueur non trouvé avec id $playerId")
            return totalScore
        }

        // Récupérer les points déjà enregistrés dans la base de données
        val playerPoints = playerDocument.getLong("points")?.toInt() ?: 0
        totalScore = playerPoints
        Log.e("calculatePlayerScore", "Score initial du joueur : $totalScore")

        // Parcourir les suggestions du joueur
        for (document in suggestionsQuery.documents) {
            val suggestionPlayerId = document.getString("playerId")
            if (suggestionPlayerId == null || suggestionPlayerId != playerId) continue

            val word = document.getString("word") ?: continue

            val wordInfo = getWordInfo(lobbyId,playerId, word) ?: continue

            Log.d("calculatePlayerScore", "Suggestion : ${wordInfo.word}, isFound: ${wordInfo.isFound}, trouvé par: ${wordInfo.firstPlayerId}")

            if (wordInfo.isFound && wordInfo.firstPlayerId == playerId) {
                totalScore += wordInfo.points
            }
        }

        Log.e("calculatePlayerScore", "Score final calculé : $totalScore")

        // Mettre à jour le score du joueur dans la base de données
        playersCollection.document(playerId).update("points", totalScore).await()

        return totalScore
    }


    override suspend fun resetGame(lobbyId: String, playerId: String) {
        // Récupére et affiche les 4 mots qu'il fallait trouver
        val wordsQuery = wordsSelectedCollection.whereEqualTo("lobbyId", lobbyId).get().await()
        wordsQuery.documents.mapNotNull { it.getString("word") }
    }

    override suspend fun getWordsToFind(lobbyId: String): List<String> {
        val wordsQuery = wordsSelectedCollection.whereEqualTo("lobbyId", lobbyId).get().await()
        return wordsQuery.documents.mapNotNull { it.getString("word") }
    }

    override suspend fun getTotalPoints(lobbyId: String, playerId: String): Int {
        var totalScore = 0
        val countedWords = mutableSetOf<String>()

        try {
            val suggestionsQuery = playerSuggestionsCollection
                .whereEqualTo("lobbyId", lobbyId)
                .get()
                .await()

            for (document in suggestionsQuery.documents) {
                val word = document.getString("word") ?: continue

                // Si le mot a déjà été compté, on passe au suivant
                if (countedWords.contains(word)) continue

                val wordInfo = getWordInfo(lobbyId, playerId, word) ?: continue

                if (wordInfo.isFound) {
                    totalScore += wordInfo.points
                    countedWords.add(word) // On ajoute le mot à la liste des mots déjà comptés
                }
            }
        } catch (e: Exception) {
            Log.e("getTotalPoints", "Erreur lors du calcul du score : ${e.message}")
        }

        return totalScore
    }

    override suspend fun getTotalPlayerPoints(lobbyId: String, playerId: String): Int {
        var totalScore = 0
        try {
            val suggestionsQuery = playerSuggestionsCollection
                .whereEqualTo("lobbyId", lobbyId)
                .whereEqualTo("playerId", playerId)
                .get()
                .await()

            for (document in suggestionsQuery.documents) {
                val word = document.getString("word") ?: continue
                val wordInfo = getWordInfo(lobbyId, playerId,word) ?: continue

                if (wordInfo.isFound && wordInfo.firstPlayerId == playerId) {
                    totalScore += wordInfo.points
                }
            }
        } catch (e: Exception) {
            Log.e("getTotalPoints", "Erreur lors du calcul du score : ${e.message}")
        }
        return totalScore
    }

    override suspend fun deletePlayerWordSuggestion(pseudo: String) {
        val querySnapshot = playerSuggestionsCollection.whereEqualTo("playerPseudo", pseudo).get().await()
        querySnapshot.forEach { suggestion ->
            playerSuggestionsCollection.document(suggestion.id).delete().await()
        }
    }

    override suspend fun clearPlayerSuggestions(lobbyId: String) {
        val collection = db.collection("player_suggestions")
        val querySnapshot = collection.whereEqualTo("lobbyId", lobbyId).get().await()
        for (document in querySnapshot.documents) {
            document.reference.delete().await()
        }
    }

    override suspend fun clearSelectedWords(lobbyId: String) {
        val collection = db.collection("words_selected")
        val querySnapshot = collection.whereEqualTo("lobbyId", lobbyId).get().await()
        for (document in querySnapshot.documents) {
            document.reference.delete().await()
        }
    }
}
