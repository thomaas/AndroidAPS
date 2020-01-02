package info.nightscout.androidaps.plugins.general.open_humans.properties

import android.content.SharedPreferences
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class UploadCounterProperty(
        private val sharedPreferences: SharedPreferences,
        private val projectMemberIdProperty: KProperty0<String?>
): ObservableProperty<Int>(sharedPreferences.getInt("Counter_${projectMemberIdProperty.get()}", 0)) {

    override fun afterChange(property: KProperty<*>, oldValue: Int, newValue: Int) {
        sharedPreferences.edit().putInt("Counter_${projectMemberIdProperty.get()}", newValue).apply()
        super.afterChange(property, oldValue, newValue)
    }

}