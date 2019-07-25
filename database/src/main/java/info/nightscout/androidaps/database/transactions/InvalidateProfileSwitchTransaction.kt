package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class InvalidateProfileSwitchTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val profileSwitch = AppRepository.database.profileSwitchDao.findById(id)
                ?: throw IllegalArgumentException("There is no such ProfileSwitch with the specified ID.")
        profileSwitch.valid = false
        AppRepository.database.profileSwitchDao.updateExistingEntry(profileSwitch)
        changes.add(profileSwitch)
    }
}