package info.nightscout.androidaps.plugins.general.open_humans.properties

import android.content.SharedPreferences
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class ProjectMemberIdProperty(
        private val sharedPreferences: SharedPreferences
) : ObservableProperty<String?>(sharedPreferences.getString("projectMemberId", null)) {

    override fun afterChange(property: KProperty<*>, oldValue: String?, newValue: String?) {
        sharedPreferences.edit().putString("projectMemberId", newValue).apply()
        super.afterChange(property, oldValue, newValue)
    }

    companion object {
    }
}