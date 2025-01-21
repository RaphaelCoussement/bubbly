package org.raphou.domain.repositories

import org.raphou.domain.models.Theme

interface IThemeRepository {
    suspend fun getPopularThemes(): List<Theme>
}