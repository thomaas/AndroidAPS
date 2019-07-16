package info.nightscout.androidaps.database

import android.content.Context
import androidx.room.Room
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.entities.TherapyEvent
import info.nightscout.androidaps.database.transactions.InsightHistoryTransaction
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object AppRepository {

    private const val DB_FILE = "AndroidAPS.db"

    internal lateinit var database: AppDatabase;

    fun initialize(context: Context) {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE).build()
    }

    fun createOrUpdateBasedOnTimestamp(glucoseValue: GlucoseValue): Single<Boolean> = Single.fromCallable {
        database.glucoseValueDao.createOrUpdateBasedOnTimestamp(glucoseValue)
    }.subscribeOn(Schedulers.io())

    fun update(glucoseValue: GlucoseValue): Single<Long> = Single.fromCallable {
        database.glucoseValueDao.updateExistingEntry(glucoseValue)
    }

    fun getLastGlucoseValue(): Maybe<GlucoseValue> = database.glucoseValueDao.getLastGlucoseValue().subscribeOn(Schedulers.io())

    fun getLastRecentGlucoseValue(): Maybe<GlucoseValue> = System.currentTimeMillis().let {
        database.glucoseValueDao.getLastGlucoseValueInTimeRange(it - TimeUnit.MINUTES.toMillis(9), it)
    }.subscribeOn(Schedulers.io())

    fun getGlucoseValuesInTimeRange(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getProperGlucoseValuesInTimeRange(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getProperGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getProperGlucoseValuesInTimeRange(timeRange: Long): Flowable<List<GlucoseValue>> = database.glucoseValueDao.getProperGlucoseValuesInTimeRange(timeRange).subscribeOn(Schedulers.io())

    fun insert(therapyEvent: TherapyEvent): Single<Long> = Single.fromCallable {
        database.therapyEventDao.insertNewEntry(therapyEvent)
    }.subscribeOn(Schedulers.io())

    fun processInsightHistoryTransaction(transaction: InsightHistoryTransaction): Completable = InsightHistoryTransaction.process(transaction)
}