package info.nightscout.androidaps.plugins.general.open_humans.properties

import android.content.SharedPreferences
import info.nightscout.androidaps.plugins.general.open_humans.OpenHumansAPI
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class OAuthTokenProperty(
        private val sharedPreferences: SharedPreferences
) : ObservableProperty<OpenHumansAPI.OAuthTokens?>(if (sharedPreferences.contains("accessToken")) {
    OpenHumansAPI.OAuthTokens(
            sharedPreferences.getString("accessToken", null)!!,
            sharedPreferences.getString("refreshToken", null)!!,
            sharedPreferences.getLong("expiresAt", 0)
    )
} else {
    null
}) {

    override fun afterChange(property: KProperty<*>, oldValue: OpenHumansAPI.OAuthTokens?, newValue: OpenHumansAPI.OAuthTokens?) {
        if (newValue == null) {
            sharedPreferences.edit()
                    .remove("accessToken")
                    .remove("refreshToken")
                    .remove("expiresAt")
                    .apply()
        } else {
            sharedPreferences.edit()
                    .putString("accessToken", newValue.accessToken)
                    .putString("refreshToken", newValue.refreshToken)
                    .putLong("expiresAt", newValue.expiresAt)
                    .apply()
        }
        super.afterChange(property, oldValue, newValue)
    }

}