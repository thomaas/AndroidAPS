package info.nightscout.androidaps.database.transactions.combo

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.transactions.Transaction

class ComboCancelTempBasalTransaction: Transaction<Unit>() {

    override fun run() {
        val now = System.currentTimeMillis()
        val currentlyActive = AppRepository.database.temporaryBasalDao
                .getTemporaryBasalActiveAt(now, InterfaceIDs.PumpType.ACCU_CHEK_COMBO)
                ?: throw IllegalStateException("There is currently no TemporaryBasal active.")
        currentlyActive.duration = now - currentlyActive.timestamp
        AppRepository.database.temporaryBasalDao.updateExistingEntry(currentlyActive)
        changes.add(currentlyActive)
    }
}
