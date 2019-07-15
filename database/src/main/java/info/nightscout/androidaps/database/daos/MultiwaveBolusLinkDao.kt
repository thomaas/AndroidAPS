package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_MULTIWAVE_BOLUS_LINKS
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink

@Suppress("FunctionName")
@Dao
abstract class MultiwaveBolusLinkDao : BaseDao<MultiwaveBolusLink>() {

    @Query("SELECT * FROM $TABLE_MULTIWAVE_BOLUS_LINKS WHERE id = :id")
    abstract override fun findById(id: Long): MultiwaveBolusLink?

    @Query("SELECT * FROM $TABLE_MULTIWAVE_BOLUS_LINKS WHERE pumpType = :pumpType AND pumpSerial = :pumpSerial AND pumpId = :pumpId AND startId IS NULL AND endId IS NULL")
    abstract fun findByPumpId_StartAndEndIDsAreNull(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long?): MultiwaveBolusLink?

    @Query("SELECT * FROM $TABLE_MULTIWAVE_BOLUS_LINKS WHERE pumpType = :pumpType AND pumpId = :pumpId AND pumpSerial = :pumpSerial AND startId IS NOT NULL AND endId IS NULL")
    abstract fun findByPumpId_StartIdIsNotNull_EndIdIsNull(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long?): MultiwaveBolusLink?
}