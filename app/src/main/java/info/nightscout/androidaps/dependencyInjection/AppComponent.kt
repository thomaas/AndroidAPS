package info.nightscout.androidaps.dependencyInjection

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.dependencyInjection.networking.NetModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        ActivitiesModule::class,
        FragmentsModule::class,
        NetModule::class,
        AppModule::class
    ]
)
interface AppComponent : AndroidInjector<MainApp> {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }
}