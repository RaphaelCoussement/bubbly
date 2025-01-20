package org.raphou.data.di

import org.koin.dsl.module
import org.raphou.data.repositories.ThemeRepositoryImpl
import org.raphou.domain.repositories.IThemeRepository

val dataModule = module {

    single<IThemeRepository> {
        ThemeRepositoryImpl()
    }
}