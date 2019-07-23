package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class CancelTemporaryTargetTransaction : Transaction<Unit>() {

    override fun run() {
        val currentTimeMillis = System.currentTimeMillis()
        val currentlyActive = AppRepository.database.temporaryTargetDao.getTemporaryTargetActiveAt(currentTimeMillis)
                ?: throw IllegalStateException("There is currently no TemporaryTarget active.")
        currentlyActive.duration = currentTimeMillis - currentlyActive.timestamp
        AppRepository.database.temporaryTargetDao.updateExistingEntry(currentlyActive)
        changes.add(currentlyActive)
    }
}