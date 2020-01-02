package info.nightscout.androidaps.database.transactions.preferences

import info.nightscout.androidaps.database.entities.PreferenceChange
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class SafePreferenceChangeTransaction(
        private val key: String,
        private val value: Any?
) : Transaction<Unit>() {
    override fun run() {
        val timestamp = System.currentTimeMillis()
        database.preferenceChangeDao.insert(PreferenceChange(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                key = key,
                value = value
        ))
    }
}