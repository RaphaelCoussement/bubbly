package org.raphou.data.repositories

import org.raphou.domain.models.Theme
import org.raphou.domain.repositories.IThemeRepository

class ThemeRepositoryImpl : IThemeRepository {
    override suspend fun getPopularThemes(): List<Theme> {
        return listOf(
            Theme.CINEMA,
            Theme.VOYAGE,
            Theme.MUSIQUE,
            Theme.JEUX_VIDEO,
            Theme.RETRO
        )
    }
}