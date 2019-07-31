package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_APS_RESULTS
import info.nightscout.androidaps.database.entities.APSResult

@Suppress("FunctionName")
@Dao
abstract class APSResultDao : BaseDao<APSResult>() {

    @Query("SELECT * FROM $TABLE_APS_RESULTS WHERE id = :id")
    abstract override fun findById(id: Long): APSResult?

    @Query("DELETE FROM $TABLE_APS_RESULTS")
    abstract override fun deleteAllEntries()
}