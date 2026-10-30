package incident.android

import android.app.Application
import incident.android.di.dataModule
import incident.android.di.networkModule
import incident.android.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            modules(networkModule, dataModule, viewModelModule)
        }
    }
}
