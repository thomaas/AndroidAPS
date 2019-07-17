package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_MULTIWAVE_BOLUS_LINKS
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink

@Suppress("FunctionName")
@Dao
abstract class MultiwaveBolusLinkDao : BaseDao<MultiwaveBolusLink>() {

    @Query("SELECT * FROM $TABLE_MULTIWAVE_BOLUS_LINKS WHERE id = :id")
    abstract override fun findById(id: Long): MultiwaveBolusLink?

    @Query("SELECT * FROM $TABLE_MULTIWAVE_BOLUS_LINKS WHERE bolusId = :bolusId AND extendedBolusId = :extendedBolusId AND referenceId IS NULL")
    abstract fun findByBolusIDs(bolusId: Long, extendedBolusId: Long): MultiwaveBolusLink?
}