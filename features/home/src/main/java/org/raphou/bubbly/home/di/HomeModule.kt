package org.raphou.bubbly.home.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.raphou.bubbly.home.HomeScreenViewModel
import org.raphou.domain.repositories.IThemeRepository


val homeModule = module {
    viewModel { HomeScreenViewModel(get<IThemeRepository>()) }

}