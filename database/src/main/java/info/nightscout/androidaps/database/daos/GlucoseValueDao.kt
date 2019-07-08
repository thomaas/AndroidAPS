package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_GLUCOSE_VALUES
import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.Maybe

@Dao
abstract class GlucoseValueDao : BaseDao<GlucoseValue>() {

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE id = :id")
    abstract override fun findById(id: Long): GlucoseValue?

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE timestamp = :timestamp")
    abstract fun findByTimestamp(timestamp: Long) : Maybe<GlucoseValue>

}