package info.nightscout.androidaps.database

import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.schedulers.Schedulers

@Deprecated(
        message = "This class only adds support for blocking calls while migrating to reactive application design. Avoid using it.",
        replaceWith = ReplaceWith("AppRepository", "info.nightscout.androidaps.database.AppRepository")
)
object BlockingAppRepository {

    fun createOrUpdateBasedOnTimestamp(glucoseValue: GlucoseValue): Boolean = AppRepository.createOrUpdateBasedOnTimestamp(glucoseValue).subscribeOn(Schedulers.io()).blockingGet()

    fun getLastGlucoseValue(): GlucoseValue? = AppRepository.getLastGlucoseValue().subscribeOn(Schedulers.io()).blockingGet()

    fun getLastRecentGlucoseValue(): GlucoseValue? = AppRepository.getLastRecentGlucoseValue().subscribeOn(Schedulers.io()).blockingGet()

    fun getGlucoseValuesInTimeRange(start: Long, end: Long): List<GlucoseValue> = AppRepository.getGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io()).blockingGet()

    fun getProperGlucoseValuesInTimeRange(start: Long, end: Long): List<GlucoseValue> = AppRepository.getProperGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io()).blockingGet()

    fun update(glucoseValue: GlucoseValue) = AppRepository.update(glucoseValue).subscribeOn(Schedulers.io()).blockingAwait()
}