package info.nightscout.androidaps.database

import android.content.Context
import androidx.room.Room
import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.Single

object AppRepository {

    private const val DB_FILE = "AndroidAPS.db"

    private lateinit var database: AppDatabase;

    fun initialize(context: Context) {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE).build()
    }

    fun createOrUpdateBasedOnTimestamp(glucoseValue: GlucoseValue): Single<Boolean> {
        return database.glucoseValueDao.findByTimestamp(glucoseValue.timestamp)
                .materialize()
                .flatMap {
                    when {
                        it.value == null -> database.glucoseValueDao.insertNewEntry(glucoseValue).map { true }
                        it.value!!.contentEqualsTo(glucoseValue) -> Single.just(false)
                        else -> database.glucoseValueDao.updateExistingEntry(glucoseValue.copy(id = it.value!!.id)).map { true }
                    }
                }
    }

    fun insertNewEntry(entry: GlucoseValue) = database.glucoseValueDao.insertNewEntry(entry)

    fun updateExistingEntry(entry: GlucoseValue) = database.glucoseValueDao.updateExistingEntry(entry)
}