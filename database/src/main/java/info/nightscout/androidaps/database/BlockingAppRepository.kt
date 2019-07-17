package info.nightscout.androidaps.database

import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.entities.TherapyEvent
import info.nightscout.androidaps.database.transactions.Transaction

@Deprecated(
        message = "This class only adds support for blocking calls while migrating to reactive application design. Avoid using it.",
        replaceWith = ReplaceWith("AppRepository", "info.nightscout.androidaps.database.AppRepository")
)
object BlockingAppRepository {

    fun createOrUpdateBasedOnTimestamp(glucoseValue: GlucoseValue): Boolean = AppRepository.createOrUpdateBasedOnTimestamp(glucoseValue).blockingGet()

    fun getLastGlucoseValue(): GlucoseValue? = AppRepository.getLastGlucoseValue().blockingGet()

    fun getLastRecentGlucoseValue(): GlucoseValue? = AppRepository.getLastRecentGlucoseValue().blockingGet()

    fun getGlucoseValuesInTimeRange(start: Long, end: Long): List<GlucoseValue> = AppRepository.getGlucoseValuesInTimeRange(start, end).blockingGet()

    fun getProperGlucoseValuesInTimeRange(start: Long, end: Long): List<GlucoseValue> = AppRepository.getProperGlucoseValuesInTimeRange(start, end).blockingGet()

    fun insert(therapyEvent: TherapyEvent): Long = AppRepository.insert(therapyEvent).blockingGet()

    fun update(glucoseValue: GlucoseValue): Long = AppRepository.update(glucoseValue).blockingGet()

    fun <T> runTransaction(transaction: Transaction<T>) = AppRepository.runTransaction(transaction).blockingAwait()

    fun <T> runTransactionForResult(transaction: Transaction<T>): T = AppRepository.runTransactionForResult(transaction).blockingGet()
}