package info.nightscout.androidaps.database.daos

import androidx.room.*
import info.nightscout.androidaps.database.TABLE_GLUCOSE_VALUES
import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
abstract class GlucoseValueDao : BaseDao<GlucoseValue>() {

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE id = :id")
    abstract override fun findById(id: Long): GlucoseValue

}