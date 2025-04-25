package org.raphou.bubbly.domain.home

interface IUserPreferencesRepository {
    suspend fun savePseudo(pseudo: String)
    suspend fun getPseudo(): String?
    suspend fun clearPseudo()
}