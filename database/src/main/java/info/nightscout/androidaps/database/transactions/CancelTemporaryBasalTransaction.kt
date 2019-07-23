package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class CancelTemporaryBasalTransaction: Transaction<Unit>() {

    override fun run() {
        val currentTimeMillis = System.currentTimeMillis()
        val currentlyActive = AppRepository.database.temporaryBasalDao.getTemporaryBasalActiveAt(currentTimeMillis)
                ?: throw IllegalStateException("There is currently no TemporaryBasal active.")
        currentlyActive.duration = currentTimeMillis - currentlyActive.timestamp
        AppRepository.database.temporaryBasalDao.updateExistingEntry(currentlyActive)
        changes.add(currentlyActive)
    }
}