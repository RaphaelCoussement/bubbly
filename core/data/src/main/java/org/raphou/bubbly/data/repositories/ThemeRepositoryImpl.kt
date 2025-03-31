package org.raphou.bubbly.data.repositories

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import org.raphou.bubbly.domain.exceptions.ThemeException
import org.raphou.bubbly.domain.theme.Theme
import org.raphou.domain.repositories.IThemeRepository

class ThemeRepositoryImpl : IThemeRepository {
    private val db = Firebase.firestore
    private val themesCollection = db.collection("themes")

    override suspend fun getPopularThemes(): List<Theme> {
        return try {
            val existingThemes = themesCollection.get().await()
            if (existingThemes.isEmpty) {
                val defaultThemes = listOf(
                    Theme("1", "Animaux"),
                    Theme("2", "Nourriture"),
                    Theme("3", "Sport"),
                    Theme("4", "Voyage"),
                    Theme("5", "Objet")
                )

                try {
                    defaultThemes.forEach { theme ->
                        themesCollection.document(theme.id).set(theme).await()
                    }
                    Log.d("ThemeRepository", "Collection 'themes' créée avec succès.")
                } catch (e: Exception) {
                    Log.e("ThemeRepository", "Erreur lors de l'insertion des thèmes par défaut", e)
                    throw ThemeException.DefaultThemesInsertionFailed("Erreur lors de l'insertion des thèmes par défaut.", e)
                }
            }

            themesCollection.get().await().documents.mapNotNull { doc ->
                doc.toObject(Theme::class.java)?.copy(id = doc.id)
            }

        } catch (e: ThemeException.DefaultThemesInsertionFailed) {
            Log.e("ThemeRepository", "Erreur d'insertion des thèmes par défaut", e)
            throw e
        } catch (e: FirebaseFirestoreException) {
            Log.e("ThemeRepository", "Erreur lors de la récupération des thèmes depuis Firestore", e)
            throw ThemeException.ThemeRetrievalFailed("Erreur lors de la récupération des thèmes depuis Firestore.", e)
        } catch (e: Exception) {
            Log.e("ThemeRepository", "Erreur lors de la récupération des thèmes", e)
            throw ThemeException.ThemeRetrievalFailed("Erreur inconnue lors de la récupération des thèmes.", e)
        }
    }

}
