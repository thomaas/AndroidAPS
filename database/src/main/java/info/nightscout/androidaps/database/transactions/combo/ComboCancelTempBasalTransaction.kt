package info.nightscout.androidaps.database.transactions.combo

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.TemporaryBasal
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class ComboCancelTempBasalTransaction: Transaction<Unit>() {

    override fun run() {
        val now = System.currentTimeMillis();
        AppRepository.database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                utcOffset = TimeZone.getDefault().getOffset(now).toLong(),
                timestamp = now,
                absolute = false,
                rate = 100.0,
                duration = 0,
                type = TemporaryBasal.Type.NORMAL
        ).apply {
            changes.add(this)
        })
    }
}
