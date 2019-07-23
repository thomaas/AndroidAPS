package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class CancelExtendedBolusTransaction : Transaction<Unit>() {

    override fun run() {
        val currentTimeMillis = System.currentTimeMillis()
        val currentlyActive = AppRepository.database.extendedBolusDao.getExtendedBolusActiveAt(currentTimeMillis)
                ?: throw IllegalStateException("There is currently no ExtendedBolus active.")
        currentlyActive.duration = currentTimeMillis - currentlyActive.timestamp
        AppRepository.database.extendedBolusDao.updateExistingEntry(currentlyActive)
        changes.add(currentlyActive)
    }
}