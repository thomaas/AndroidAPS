package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class InvalidateTemporaryBasalTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val tempBasal = AppRepository.database.temporaryBasalDao.findById(id)
                ?: throw IllegalArgumentException("There is no such TemporaryBasal with the specified ID.")
        tempBasal.valid = false
        AppRepository.database.temporaryBasalDao.updateExistingEntry(tempBasal)
        changes.add(tempBasal)
    }
}