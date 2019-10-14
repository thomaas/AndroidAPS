package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_APS_RESULTS
import info.nightscout.androidaps.database.entities.APSResult
import io.reactivex.Single

@Suppress("FunctionName")
@Dao
internal interface APSResultDao : BaseDao<APSResult> {

    @Query("SELECT * FROM $TABLE_APS_RESULTS WHERE id = :id")
    override fun findById(id: Long): APSResult?

    @Query("DELETE FROM $TABLE_APS_RESULTS")
    override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_APS_RESULTS WHERE dateCreated >= :start AND dateCreated < :end ORDER BY dateCreated ASC")
    override fun getAllEntriesCreatedBetween(start: Long, end: Long): Single<List<APSResult>>
}