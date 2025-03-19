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
import org.raphou.bubbly.domain.word.Word
import java.io.InputStream
import kotlin.random.Random

class WordRepositoryImpl(private val context: Context) : IWordRepository {
    private val db = Firebase.firestore
    private val wordsCollection = db.collection("words")

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

    override suspend fun getRandomWords(theme: String?): List<Word> {
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
                    words.add(wordsList[Random.nextInt(wordsList.size)])
                }
            }
            words
        } catch (e: Exception) {
            Log.e("WordRepository", "Erreur lors de la récupération des mots", e)
            throw WordException.WordRetrievalFailed("Erreur lors de la récupération des mots depuis la base de données.", e)
        }
    }
}
