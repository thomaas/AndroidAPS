package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import info.nightscout.androidaps.database.TABLE_GLUCOSE_VALUES
import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
abstract class GlucoseValueDao : BaseDao<GlucoseValue>() {

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE id = :id")
    abstract override fun findById(id: Long): GlucoseValue?

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp = :timestamp AND referenceID IS NULL")
    abstract fun findByTimestamp(timestamp: Long): GlucoseValue?

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE referenceID IS NULL AND valid = 1 ORDER BY timestamp DESC LIMIT 1")
    abstract fun getLastGlucoseValue() : Maybe<GlucoseValue>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= :start AND timestamp <= :end AND referenceID IS NULL AND valid = 1 ORDER BY timestamp DESC LIMIT 1")
    abstract fun getLastGlucoseValueInTimeRange(start: Long, end: Long) : Maybe<GlucoseValue>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= :start AND timestamp <= :end AND referenceID IS NULL AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getGlucoseValuesInTimeRange(start: Long, end: Long) : Single<List<GlucoseValue>>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= :start AND timestamp <= :end AND value >= 39 AND referenceID IS NULL AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getProperGlucoseValuesInTimeRange(start: Long, end: Long) : Single<List<GlucoseValue>>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= (julianday('now') - 2440587.5) * 86400000.0 - :timeRange AND timestamp < (julianday('now') - 2440587.5) * 86400000.0 AND referenceID IS NULL AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getProperGlucoseValuesInTimeRange(timeRange: Long) : Flowable<List<GlucoseValue>>

    @Transaction
    open fun createOrUpdateBasedOnTimestamp(glucoseValue: GlucoseValue): Boolean {
        val current = findByTimestamp(glucoseValue.timestamp)
        return when {
            current == null -> {
                insertNewEntry(glucoseValue)
                true
            }
            current.contentEqualsTo(glucoseValue) -> {
                false
            }
            else -> {
                glucoseValue.id = current.id
                updateExistingEntry(glucoseValue)
                true
            }
        }
    }
}