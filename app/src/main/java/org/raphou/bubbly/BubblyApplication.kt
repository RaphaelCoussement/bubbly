package org.raphou.bubbly

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.raphou.bubbly.data.di.dataModule
import org.raphou.bubbly.home.di.homeModule


class BubblyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@BubblyApplication)
            modules(
                dataModule,
                homeModule
            )
        }
    }
}
