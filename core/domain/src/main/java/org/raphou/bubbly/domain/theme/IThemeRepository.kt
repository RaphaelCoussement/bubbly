package org.raphou.domain.repositories

import org.raphou.bubbly.domain.theme.Theme

interface IThemeRepository {
    suspend fun getPopularThemes(): List<Theme>
}