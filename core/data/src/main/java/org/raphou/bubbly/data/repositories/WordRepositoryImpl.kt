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

    private fun parseWordsFromJson(jsonString: String): List<Word> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonObject = JSONObject(jsonString)

        val words = mutableListOf<Word>()

        // Parcours des thèmes et des difficultés
        jsonObject.keys().forEach { theme ->
            val difficulties = jsonObject.getJSONObject(theme)
            difficulties.keys().forEach { difficulty ->
                val wordsArray = difficulties.getJSONArray(difficulty)
                for (i in 0 until wordsArray.length()) {
                    words.add(
                        Word(
                            name = wordsArray.getString(i),
                            theme = theme,
                            difficulty = Difficulty.valueOf(difficulty.toUpperCase())
                        )
                    )
                }
            }
        }
        return words
    }

    private fun generateWordId(word: Word): String {
        return "${word.theme}_${word.difficulty}_${word.name.hashCode()}"
    }

    override suspend fun getRandomWords(theme: String?, lobbyId: String): List<Word> {
        return try {
            val difficulties = Difficulty.values()
            val words = mutableListOf<Word>()

            for (difficulty in difficulties) {
                val query = if (theme != null) {
                    wordsCollection.whereEqualTo("theme", theme).whereEqualTo("difficulty", difficulty.name)
                } else {
                    wordsCollection.whereEqualTo("difficulty", difficulty.name)
                }

                val result = query.get().await()
                val wordsList = result.documents.mapNotNull { it.toObject(Word::class.java) }
                if (wordsList.isNotEmpty()) {
                    val selectedWord = wordsList[Random.nextInt(wordsList.size)]
                    words.add(selectedWord)

                    // Détermine les points en fonction de la difficulté
                    val points = when (difficulty) {
                        Difficulty.FACILE -> 1
                        Difficulty.MOYEN -> 2
                        Difficulty.DIFFICILE -> 3
                        Difficulty.EXTREME -> 4
                    }

                    val chosenWord = mapOf(
                        "lobbyId" to lobbyId,
                        "word" to selectedWord.name,
                        "difficulty" to difficulty.name,
                        "points" to points,
                        "isFound" to false
                    )

                    wordsSelectedCollection.add(chosenWord).await()
                }
            }
            words
        } catch (e: Exception) {
            Log.e("WordRepository", "Erreur lors de la récupération des mots", e)
            throw WordException.WordRetrievalFailed("Erreur lors de la récupération des mots depuis la base de données.", e)
        }
    }

    override suspend fun addWordSelected(lobbyId: String, word: String, isFound: Boolean, points: Int) {
        try {
            val selectedWord = WordSelected(lobbyId, word, isFound, points)
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
                if (isSimilar(selectedWord, word)) {
                    // MAJ mot comme trouvé
                    document.reference.update("isFound", true).await()
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

        for (i in s1.indices) dp[i][0] = i
        for (j in s2.indices) dp[0][j] = j

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
        return levenshteinDistance(normalized1, normalized2) <= 1 // Tolérance de 1 erreur
    }


    override suspend fun getWordInfo(lobbyId: String, word: String): WordSelected? {
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
                    points = wordDocument.getLong("points")?.toInt() ?: 0
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
                        points = doc.getLong("points")?.toInt() ?: 0
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
        val suggestionsQuery = playerSuggestionsCollection.whereEqualTo("lobbyId", lobbyId).get().await()

        for (document in suggestionsQuery.documents) {
            val suggestionPlayerId = document.getString("playerId")
            if (suggestionPlayerId == null) {
                Log.e("suggestionPlayerId", "Erreur: playerId est nul")
                continue
            }

            if (suggestionPlayerId != playerId) continue

            val word = document.getString("word")
            if (word == null) {
                Log.e("word", "Erreur: word est nul")
                continue
            }

            val wordInfo = getWordInfo(lobbyId, word)
            if (wordInfo == null) {
                Log.e("wordInfo", "Erreur: wordInfo est nul pour le mot $word")
                continue
            }

            Log.d("suggestionPlayerId", "wordInfo: ${wordInfo.word}")

            if (wordInfo.isFound) {
                totalScore += wordInfo.points
            }
        }
        return totalScore
    }

    override suspend fun resetGame(lobbyId: String, playerId: String) {

        // Récupére et affiche les 4 mots qu'il fallait trouver
        val wordsQuery = wordsSelectedCollection.whereEqualTo("lobbyId", lobbyId).get().await()
        val wordsToFind = wordsQuery.documents.mapNotNull { it.getString("word") }

        Log.d("resetGame", "Appel de calculatePlayerScore avec lobbyId: $lobbyId, playerId: $playerId")

        // Calcul du score
        val scores = calculatePlayerScore(lobbyId, playerId)

        Log.d("WordRepository", "Fin de manche - Mots: $wordsToFind, Scores: $scores")
    }

    override suspend fun getWordsToFind(lobbyId: String): List<String> {
        val wordsQuery = wordsSelectedCollection.whereEqualTo("lobbyId", lobbyId).get().await()
        return wordsQuery.documents.mapNotNull { it.getString("word") }
    }

}
