package org.raphou.bubbly.data.repositories

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import org.raphou.bubbly.domain.theme.Theme
import org.raphou.domain.repositories.IThemeRepository

class ThemeRepositoryImpl : IThemeRepository {
    private val db = Firebase.firestore
    private val themesCollection = db.collection("themes")

    override suspend fun getPopularThemes(): List<Theme> {
        return try {
            // Vérifier si la collection est vide
            val existingThemes = themesCollection.get().await()
            if (existingThemes.isEmpty) {
                // Insérer les thèmes par défaut si la collection est vide
                val defaultThemes = listOf(
                    Theme("1", "Culture Pop"),
                    Theme("2", "Nourriture"),
                    Theme("3", "Sport"),
                    Theme("4", "Voyage"),
                    Theme("5", "Objet")
                )

                defaultThemes.forEach { theme ->
                    themesCollection.document(theme.id).set(theme).await()
                }

                Log.d("ThemeRepository", "Collection 'themes' créée avec succès.")
            }

            // Récupérer tous les thèmes depuis Firestore
            themesCollection.get().await().documents.mapNotNull { doc ->
                doc.toObject(Theme::class.java)?.copy(id = doc.id)
            }

        } catch (e: Exception) {
            Log.e("ThemeRepository", "Erreur lors de la récupération des thèmes", e)
            emptyList()
        }
    }
}
