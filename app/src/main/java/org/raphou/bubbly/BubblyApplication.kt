package org.raphou.bubbly

import android.app.Application
import com.google.android.gms.ads.MobileAds
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.raphou.bubbly.data.di.dataModule


class BubblyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)

        startKoin {
            androidContext(this@BubblyApplication)
            modules(
                dataModule
            )
        }
    }
}
