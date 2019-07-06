package info.nightscout.androidaps.database.daos

import androidx.room.Insert
import androidx.room.Transaction
import androidx.room.Update
import info.nightscout.androidaps.database.interfaces.DBEntry
import io.reactivex.Completable
import io.reactivex.Single

abstract class BaseDao<T : DBEntry> {

    abstract fun findById(id: Long): T

    @Insert
    abstract fun insert(entry: T): Long

    @Update
    abstract fun update(entry: T)

    fun insertNewEntry(entry: T): Single<Long> {
        if (entry.id != 0L) throw IllegalArgumentException("ID must be 0.")
        if (entry.version != 0) throw IllegalArgumentException("Version must be 0.")
        if (entry.referenceID != null) throw IllegalArgumentException("Reference ID must be null.")
        if (!entry.foreignKeysValid) throw java.lang.IllegalArgumentException("One or more foreign keys are invalid (e.g. 0 value).")
        val lastModified = System.currentTimeMillis()
        return Single.fromCallable {
            entry.lastModified = lastModified
            insert(entry)
        }
    }

    @Transaction
    open fun saveAndLogChanges(entry: T) : Long {
        val current = findById(entry.id)
        entry.version = current.version + 1
        update(entry)
        current.referenceID = entry.id
        current.id = 0
        return insert(current)
    }

    fun updateExistingEntry(entry: T): Single<Long> {
        if (entry.id == 0L) throw IllegalArgumentException("ID must not be 0.")
        if (entry.referenceID != null) throw IllegalArgumentException("Reference ID must be null.")
        if (!entry.foreignKeysValid) throw java.lang.IllegalArgumentException("One or more foreign keys are invalid (e.g. 0 value).")
        val lastModified = System.currentTimeMillis()
        return Single.fromCallable() {
            entry.lastModified = lastModified
            saveAndLogChanges(entry)
        }
    }

}