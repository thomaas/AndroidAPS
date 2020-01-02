package info.nightscout.androidaps.database.transactions.preferences

import info.nightscout.androidaps.database.entities.PreferenceChange
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class CopyMissingPreferencesToDatabaseTransaction(
        private val preferences: Map<String, Any>
) : Transaction<Unit>() {
    override fun run() {
        val timestamp = System.currentTimeMillis()
        val utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong()
        preferences.forEach {
            val change= database.preferenceChangeDao.getMostRecentWithKey(it.key)
            if (change == null || change.value != it.value) {
                database.preferenceChangeDao.insert(PreferenceChange(
                        timestamp = System.currentTimeMillis(),
                        utcOffset = utcOffset,
                        key = it.key,
                        value = it.value
                ))
            }
        }
    }
}