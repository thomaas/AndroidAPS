package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.ExtendedBolus
import java.util.*

class InsightExtendedBolusTransaction(
        val pumpSerial: String,
        val timestamp: Long,
        val amount: Double,
        val duration: Long,
        bolusId: Int,
        val emulatingTempBasal: Boolean
) : Transaction<Unit>() {

    val bolusId = bolusId.toLong()

    override fun run() {
        AppRepository.database.extendedBolusDao.insertNewEntry(ExtendedBolus(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                amount = amount,
                duration = duration,
                emulatingTempBasal = emulatingTempBasal
        ).apply {
            interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            interfaceIDs.pumpSerial = pumpSerial
            interfaceIDs.pumpId = bolusId
        })
    }
}