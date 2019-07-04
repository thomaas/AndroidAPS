package info.nightscout.androidaps.database

import android.content.Context
import androidx.room.Room
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.interfaces.DBEntry
import io.reactivex.Completable

class AppRepository(context: Context) {

    private val database : AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "AndroidAPS.db").build()

    fun insertGlucoseValues(vararg glucoseValues: GlucoseValue) : Completable {
        glucoseValues.prepareForInsert()
        return database.glucoseValueDao().insert(*glucoseValues)
    }

    fun updateGlucoseValues(vararg glucoseValues: GlucoseValue) : Completable {
        glucoseValues.prepareForUpdate()
        return Completable.create {
            database.runInTransaction {
                glucoseValues.forEach {
                    val current = database.glucoseValueDao().findById(it.id)
                    it.id = 0
                    it.version = current.version + 1
                    database.glucoseValueDao().insertNow(it)
                    current.referenceID = it.id
                    database.glucoseValueDao().updateNow(current)
                }
            }
        }
    }

    private fun Array<out DBEntry>.prepareForInsert() {
        forEach {
            if (it.id != 0L) throw IllegalArgumentException("ID must be 0.")
            if (it.version != 0) throw IllegalArgumentException("Version must be 0.")
            if (it.referenceID != 0L) throw IllegalArgumentException("Reference ID must be 0.")
        }
        val lastModified = System.currentTimeMillis()
        forEach { it.lastModified = lastModified }
    }

    private fun Array<out DBEntry>.prepareForUpdate() {
        forEach {
            if (it.id == 0L) throw IllegalArgumentException("ID must not be 0.")
            if (it.referenceID != 0L) throw IllegalArgumentException("Reference ID must be 0")
        }
        val lastModified = System.currentTimeMillis()
        forEach { it.lastModified = lastModified }
    }
}