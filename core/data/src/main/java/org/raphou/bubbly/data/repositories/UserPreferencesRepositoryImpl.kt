package org.raphou.bubbly.data.repositories

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.raphou.bubbly.domain.home.IUserPreferencesRepository

class UserPreferencesRepositoryImpl(
    private val context: Context
) : IUserPreferencesRepository {

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "user_prefs")
        private val PSEUDO_KEY = stringPreferencesKey("user_pseudo")
    }

    override suspend fun savePseudo(pseudo: String) {
        context.dataStore.edit { prefs -> prefs[PSEUDO_KEY] = pseudo }
    }

    override suspend fun getPseudo(): String? {
        return context.dataStore.data.first()[PSEUDO_KEY]
    }

    override suspend fun clearPseudo() {
        context.dataStore.edit { prefs -> prefs.remove(PSEUDO_KEY) }
    }
}