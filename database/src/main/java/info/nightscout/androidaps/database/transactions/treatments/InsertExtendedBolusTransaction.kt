package info.nightscout.androidaps.database.transactions.treatments

import info.nightscout.androidaps.database.entities.ExtendedBolus
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

/**
 * Inserts an ExtendedBolus to the database
 */
class InsertExtendedBolusTransaction(val timestamp: Long, val duration: Long, val amount: Double) : Transaction<Unit>() {
    override fun run() {
        database.extendedBolusDao.insertNewEntry(ExtendedBolus(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                amount = amount,
                duration = duration,
                isEmulatingTempBasal = false
        ))
    }
}