package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.TemporaryTarget
import java.util.*

class InsertTemporaryTargetTransaction(
        val timestamp: Long,
        val duration: Long,
        val reason: TemporaryTarget.Reason,
        val target: Double
) : Transaction<Unit>() {
    override fun run() {
        AppRepository.database.temporaryTargetDao.insertNewEntry(TemporaryTarget(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                reason = reason,
                target = target,
                duration = duration
        ).apply {
            changes.add(this)
        })
    }
}