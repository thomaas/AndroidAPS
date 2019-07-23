package info.nightscout.androidaps.database

import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.transactions.Transaction

@Deprecated(
        message = "This class only adds support for blocking calls while migrating to reactive application design. Avoid using it.",
        replaceWith = ReplaceWith("AppRepository", "info.nightscout.androidaps.database.AppRepository")
)
object BlockingAppRepository {

    fun getLastGlucoseValue(): GlucoseValue? = AppRepository.getLastGlucoseValue().blockingGet()

    fun getLastRecentGlucoseValue(): GlucoseValue? = AppRepository.getLastRecentGlucoseValue().blockingGet()

    fun getGlucoseValuesInTimeRange(start: Long, end: Long): List<GlucoseValue> = AppRepository.getGlucoseValuesInTimeRange(start, end).blockingGet()

    fun getProperGlucoseValuesInTimeRange(start: Long, end: Long): List<GlucoseValue> = AppRepository.getProperGlucoseValuesInTimeRange(start, end).blockingGet()

    fun <T> runTransaction(transaction: Transaction<T>) = AppRepository.runTransaction(transaction).blockingAwait()

    fun <T> runTransactionForResult(transaction: Transaction<T>): T = AppRepository.runTransactionForResult(transaction).blockingGet()

    fun getTemporaryBasalsInTimeRange(start: Long, end: Long): List<TemporaryBasal> = AppRepository.getTemporaryBasalsInTimeRange(start, end).blockingFirst()

    fun getExtendedBolusesInTimeRange(start: Long, end: Long): List<ExtendedBolus> = AppRepository.getExtendedBolusesInTimeRange(start, end).blockingFirst()

    fun getTemporaryTargetsInTimeRange(start: Long, end: Long): List<TemporaryTarget> = AppRepository.getTemporaryTargetsInTimeRange(start, end).blockingFirst()

    fun getTotalDailyDoses(amount: Int): List<TotalDailyDose> = AppRepository.getTotalDailyDoses(amount).blockingGet()
}