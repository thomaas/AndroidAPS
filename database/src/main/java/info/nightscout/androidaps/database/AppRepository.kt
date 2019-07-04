package info.nightscout.androidaps.database

import android.content.Context
import androidx.room.Room
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.interfaces.DBEntry
import io.reactivex.Completable

class AppRepository(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "AndroidAPS.db").build()

    fun insertGlucoseValues(vararg glucoseValues: GlucoseValue) = insert(glucoseValues, database.glucoseValueDao()::insert)

    fun updateGlucoseValues(vararg glucoseValues: GlucoseValue) =
            update(glucoseValues, {database.glucoseValueDao().insertNow(it)}, {database.glucoseValueDao().updateNow(it)},
                    {database.glucoseValueDao().findByIdNow(it)})

    private fun <T : DBEntry> insert(entries: Array<T>, insertQuery: (Array<out T>) -> Completable): Completable {
        entries.forEach {
            if (it.id != 0L) throw IllegalArgumentException("ID must be 0.")
            if (it.version != 0) throw IllegalArgumentException("Version must be 0.")
            if (it.referenceID != 0L) throw IllegalArgumentException("Reference ID must be 0.")
        }
        return Completable.create {
            val lastModified = System.currentTimeMillis()
            entries.forEach { it.lastModified = lastModified }
        }.mergeWith(insertQuery(entries))
    }

    private fun <T : DBEntry> update(entries: Array<out T>, insertQuery: (T) -> Unit, updateQuery: (T) -> Unit, findQuery: (Long) -> T) : Completable {
        entries.forEach {
            if (it.id == 0L) throw IllegalArgumentException("ID must not be 0.")
            if (it.referenceID != 0L) throw IllegalArgumentException("Reference ID must be 0")
        }
        return Completable.create {
            database.runInTransaction {
                val lastModified = System.currentTimeMillis()
                entries.forEach { it.lastModified = lastModified }
                entries.forEach {
                    val current = findQuery(it.id)
                    it.id = 0
                    it.version = current.version + 1
                    insertQuery(it)
                    current.referenceID = it.id
                    updateQuery(current)
                }
            }
        }
    }
}