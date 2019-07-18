package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_GLUCOSE_VALUES
import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
abstract class GlucoseValueDao : BaseDao<GlucoseValue>() {

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE id = :id")
    abstract override fun findById(id: Long): GlucoseValue?

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp = :timestamp AND referenceId IS NULL")
    abstract fun findByTimestamp(timestamp: Long): GlucoseValue?

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE referenceId IS NULL AND valid = 1 ORDER BY timestamp DESC LIMIT 1")
    abstract fun getLastGlucoseValue() : Maybe<GlucoseValue>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= :start AND timestamp <= :end AND referenceId IS NULL AND valid = 1 ORDER BY timestamp DESC LIMIT 1")
    abstract fun getLastGlucoseValueInTimeRange(start: Long, end: Long) : Maybe<GlucoseValue>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= :start AND timestamp <= :end AND referenceId IS NULL AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getGlucoseValuesInTimeRange(start: Long, end: Long) : Single<List<GlucoseValue>>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= :start AND timestamp <= :end AND value >= 39 AND referenceId IS NULL AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getProperGlucoseValuesInTimeRange(start: Long, end: Long) : Single<List<GlucoseValue>>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= (julianday('now') - 2440587.5) * 86400000.0 - :timeRange AND timestamp < (julianday('now') - 2440587.5) * 86400000.0 AND referenceId IS NULL AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getProperGlucoseValuesInTimeRange(timeRange: Long) : Flowable<List<GlucoseValue>>
}