package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_APS_RESULT_LINKS
import info.nightscout.androidaps.database.entities.links.APSResultLink
import io.reactivex.Single

@Suppress("FunctionName")
@Dao
internal interface APSResultLinkDao : BaseDao<APSResultLink> {

    @Query("SELECT * FROM $TABLE_APS_RESULT_LINKS WHERE id = :id")
    override fun findById(id: Long): APSResultLink?

    @Query("DELETE FROM $TABLE_APS_RESULT_LINKS")
    override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_APS_RESULT_LINKS WHERE dateCreated >= :start AND dateCreated < :end ORDER BY dateCreated ASC")
    override fun getAllEntriesCreatedBetween(start: Long, end: Long): Single<List<APSResultLink>>
}