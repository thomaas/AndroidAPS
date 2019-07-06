package info.nightscout.androidaps.database.daos

import androidx.room.*
import info.nightscout.androidaps.database.TABLE_GLUCOSE_VALUES
import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
abstract class GlucoseValueDao {

    @Insert
    abstract fun insert(vararg glucoseValues : GlucoseValue) : Completable

    @Insert
    abstract fun insertNow(vararg glucoseValues: GlucoseValue)

    @Update
    abstract fun updateNow(vararg glucoseValues : GlucoseValue)

    @Query("SELECT * FROM $TABLE_GLUCOSE_VALUES WHERE id = :id")
    abstract fun findByIdNow(id: Long) : GlucoseValue

}