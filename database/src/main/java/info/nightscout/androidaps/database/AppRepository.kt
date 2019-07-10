package info.nightscout.androidaps.database

import android.content.Context
import androidx.room.Room
import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.concurrent.TimeUnit

object AppRepository {

    private const val DB_FILE = "AndroidAPS.db"

    private lateinit var database: AppDatabase;

    var glucoseValuesChangedCallback: (() -> Unit)? = null

    fun initialize(context: Context) {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE).build()
    }

    fun createOrUpdateBasedOnTimestamp(glucoseValue: GlucoseValue): Single<Boolean> {
        return database.glucoseValueDao.findByTimestamp(glucoseValue.timestamp)
                .materialize()
                .flatMap {
                    when {
                        it.value == null -> database.glucoseValueDao.insertNewEntry(glucoseValue).map {
                            glucoseValuesChangedCallback?.invoke()
                            true
                        }
                        it.value!!.contentEqualsTo(glucoseValue) -> Single.just(false)
                        else -> database.glucoseValueDao.updateExistingEntry(glucoseValue.copy(id = it.value!!.id)).map {
                            glucoseValuesChangedCallback?.invoke()
                            true
                        }
                    }
                }
    }

    fun update(glucoseValue: GlucoseValue): Completable = database.glucoseValueDao.updateCompletable(glucoseValue).doOnComplete {
        glucoseValuesChangedCallback?.invoke()
    }

    fun getLastGlucoseValue(): Maybe<GlucoseValue> = database.glucoseValueDao.getLastGlucoseValue()

    fun getLastRecentGlucoseValue(): Maybe<GlucoseValue> = System.currentTimeMillis().let {
        database.glucoseValueDao.getLastGlucoseValueInTimeRange(it - TimeUnit.MINUTES.toMillis(9), it)
    }

    fun getGlucoseValuesInTimeRange(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getGlucoseValuesInTimeRange(start, end)

    fun getProperGlucoseValuesInTimeRange(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getProperGlucoseValuesInTimeRange(start, end)
}