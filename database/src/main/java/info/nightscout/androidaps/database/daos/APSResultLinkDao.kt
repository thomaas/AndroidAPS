package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_APS_RESULT_LINKS
import info.nightscout.androidaps.database.entities.APSResult

@Suppress("FunctionName")
@Dao
abstract class APSResultLinkDao : BaseDao<APSResult>() {

    @Query("SELECT * FROM $TABLE_APS_RESULT_LINKS WHERE id = :id")
    abstract override fun findById(id: Long): APSResult?

    @Query("DELETE FROM $TABLE_APS_RESULT_LINKS")
    abstract override fun deleteAllEntries()
}