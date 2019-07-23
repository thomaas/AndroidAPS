package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class ResetDatabaseTransaction : Transaction<Unit>() {
    override fun run() {
        AppRepository.database.apsResultLinkDao.deleteAllEntries()
        AppRepository.database.bolusCalculatorResultDao.deleteAllEntries()
        AppRepository.database.bolusDao.deleteAllEntries()
        AppRepository.database.carbsDao.deleteAllEntries()
        AppRepository.database.effectiveProfileSwitchDao.deleteAllEntries()
        AppRepository.database.extendedBolusDao.deleteAllEntries()
        AppRepository.database.glucoseValueDao.deleteAllEntries()
        AppRepository.database.mealLinkDao.deleteAllEntries()
        AppRepository.database.multiwaveBolusLinkDao.deleteAllEntries()
        AppRepository.database.profileSwitchDao.deleteAllEntries()
        AppRepository.database.temporaryTargetDao.deleteAllEntries()
        AppRepository.database.temporaryBasalDao.deleteAllEntries()
        AppRepository.database.therapyEventDao.deleteAllEntries()
        AppRepository.database.totalDailyDoseDao.deleteAllEntries()
    }
}