package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_MULTIWAVE_BOLUS_LINKS
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink
import io.reactivex.Single

@Suppress("FunctionName")
@Dao
internal interface MultiwaveBolusLinkDao : BaseDao<MultiwaveBolusLink> {

    @Query("SELECT * FROM $TABLE_MULTIWAVE_BOLUS_LINKS WHERE id = :id")
    override fun findById(id: Long): MultiwaveBolusLink?

    @Query("DELETE FROM $TABLE_MULTIWAVE_BOLUS_LINKS")
    override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_MULTIWAVE_BOLUS_LINKS WHERE bolusId = :bolusId AND extendedBolusId = :extendedBolusId AND referenceId IS NULL")
    fun findByBolusIDs(bolusId: Long, extendedBolusId: Long): MultiwaveBolusLink?

    @Query("SELECT * FROM $TABLE_MULTIWAVE_BOLUS_LINKS WHERE dateCreated >= :start AND dateCreated < :end ORDER BY dateCreated ASC")
    override fun getAllEntriesCreatedBetween(start: Long, end: Long): Single<List<MultiwaveBolusLink>>
}