package info.nightscout.androidaps.database

import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.schedulers.Schedulers

object BlockingAppRepository {

    fun getLastGlucoseValue(): GlucoseValue? = AppRepository.getLastGlucoseValue().subscribeOn(Schedulers.io()).blockingGet()

    fun getLastRecentGlucoseValue(): GlucoseValue? = AppRepository.getLastRecentGlucoseValue().subscribeOn(Schedulers.io()).blockingGet()

    fun getGlucoseValuesInTimeRange(start: Long, end: Long): List<GlucoseValue> = AppRepository.getGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io()).blockingGet()

    fun getProperGlucoseValuesInTimeRange(start: Long, end: Long): List<GlucoseValue> = AppRepository.getProperGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io()).blockingGet()
}