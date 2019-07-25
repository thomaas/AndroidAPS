package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class InvalidateTherapyEventTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val therapyEvent = AppRepository.database.therapyEventDao.findById(id)
                ?: throw IllegalArgumentException("There is no such TherapyEvent with the specified ID.")
        therapyEvent.valid = false
        AppRepository.database.therapyEventDao.updateExistingEntry(therapyEvent)
        changes.add(therapyEvent)
    }
}