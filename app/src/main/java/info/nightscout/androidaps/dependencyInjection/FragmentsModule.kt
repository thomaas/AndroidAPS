package info.nightscout.androidaps.dependencyInjection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.activities.MyPreferenceFragment
import info.nightscout.androidaps.plugins.general.nsclient2.NSClient2Fragment

@Module
abstract class FragmentsModule {

    @ContributesAndroidInjector
    abstract fun contributesPreferencesFragment(): MyPreferenceFragment

    @ContributesAndroidInjector
    abstract fun contributesNSClient2Fragment(): NSClient2Fragment
}