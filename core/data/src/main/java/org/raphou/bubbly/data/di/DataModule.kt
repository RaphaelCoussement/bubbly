package org.raphou.bubbly.data.di

import org.koin.dsl.module
import org.raphou.bubbly.data.repositories.LobbyRepositoryImpl
import org.raphou.bubbly.data.repositories.ThemeRepositoryImpl
import org.raphou.bubbly.data.repositories.WordRepositoryImpl
import org.raphou.bubbly.domain.lobby.ILobbyRepository
import org.raphou.bubbly.domain.word.IWordRepository
import org.raphou.domain.repositories.IThemeRepository

val dataModule = module {

    single<IThemeRepository> {
        ThemeRepositoryImpl()
    }

    single<ILobbyRepository> {
        LobbyRepositoryImpl()
    }

    single<IWordRepository> {
        WordRepositoryImpl(get())
    }

}