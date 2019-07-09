package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_GLUCOSE_VALUES
import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
abstract class GlucoseValueDao : BaseDao<GlucoseValue>() {

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE id = :id")
    abstract override fun findById(id: Long): GlucoseValue?

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp = :timestamp AND referenceID IS NULL")
    abstract fun findByTimestamp(timestamp: Long) : Maybe<GlucoseValue>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE referenceID IS NULL AND valid = 1 ORDER BY timestamp DESC LIMIT 1")
    abstract fun getLastGlucoseValue() : Maybe<GlucoseValue>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= :start AND timestamp <= :end AND referenceID IS NULL AND valid = 1 ORDER BY timestamp DESC LIMIT 1")
    abstract fun getLastGlucoseValueInTimeRange(start: Long, end: Long) : Maybe<GlucoseValue>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= :start AND timestamp <= :end AND referenceID IS NULL AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getGlucoseValuesInTimeRange(start: Long, end: Long) : Single<List<GlucoseValue>>

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp >= :start AND timestamp <= :end AND value >= 39 AND referenceID IS NULL AND valid = 1 ORDER BY timestamp ASC")
    abstract fun getProperGlucoseValuesInTimeRange(start: Long, end: Long) : Single<List<GlucoseValue>>

}