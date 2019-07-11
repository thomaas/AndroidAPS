package info.nightscout.androidaps.database

import android.content.Context
import androidx.room.Room
import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object AppRepository {

    private const val DB_FILE = "AndroidAPS.db"

    private lateinit var database: AppDatabase;

    fun initialize(context: Context) {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE).build()
    }

    fun createOrUpdateBasedOnTimestamp(glucoseValue: GlucoseValue): Single<Boolean> {
        return database.glucoseValueDao.findByTimestamp(glucoseValue.timestamp)
                .subscribeOn(Schedulers.io())
                .materialize()
                .flatMap {
                    when {
                        it.value == null -> database.glucoseValueDao.insertNewEntry(glucoseValue).map { true }
                        it.value!!.contentEqualsTo(glucoseValue) -> Single.just(false)
                        else -> database.glucoseValueDao.updateExistingEntry(glucoseValue.copy(id = it.value!!.id)).map { true }
                    }
                }
    }

    fun update(glucoseValue: GlucoseValue): Completable = database.glucoseValueDao.updateCompletable(glucoseValue).subscribeOn(Schedulers.io())

    fun getLastGlucoseValue(): Maybe<GlucoseValue> = database.glucoseValueDao.getLastGlucoseValue().subscribeOn(Schedulers.io())

    fun getLastRecentGlucoseValue(): Maybe<GlucoseValue> = System.currentTimeMillis().let {
        database.glucoseValueDao.getLastGlucoseValueInTimeRange(it - TimeUnit.MINUTES.toMillis(9), it)
    }.subscribeOn(Schedulers.io())

    fun getGlucoseValuesInTimeRange(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getProperGlucoseValuesInTimeRange(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getProperGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getProperGlucoseValuesInTimeRange(timeRange: Long): Flowable<List<GlucoseValue>> = database.glucoseValueDao.getProperGlucoseValuesInTimeRange(timeRange).subscribeOn(Schedulers.io())
}