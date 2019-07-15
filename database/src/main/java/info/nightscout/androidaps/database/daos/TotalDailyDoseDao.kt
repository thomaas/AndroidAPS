package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_TOTAL_DAILY_DOSE
import info.nightscout.androidaps.database.entities.TotalDailyDose

@Suppress("FunctionName")
@Dao
abstract class TotalDailyDoseDao : BaseDao<TotalDailyDose>() {

    @Query("SELECT * FROM $TABLE_TOTAL_DAILY_DOSE WHERE id = :id")
    abstract override fun findById(id: Long): TotalDailyDose?
}