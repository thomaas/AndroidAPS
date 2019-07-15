package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_EXTENDED_BOLUSES
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.ExtendedBolus

@Suppress("FunctionName")
@Dao
abstract class ExtendedBolusDao : BaseDao<ExtendedBolus>() {

    @Query("SELECT * FROM $TABLE_EXTENDED_BOLUSES WHERE id = :id")
    abstract override fun findById(id: Long): ExtendedBolus?

    @Query("SELECT * FROM $TABLE_EXTENDED_BOLUSES WHERE pumpType = :pumpType AND pumpSerial = :pumpSerial AND pumpId = :pumpId AND startId IS NULL AND endId IS NULL")
    abstract fun findByPumpId_StartAndEndIDsAreNull(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long?): ExtendedBolus?

    @Query("SELECT * FROM $TABLE_EXTENDED_BOLUSES WHERE pumpType = :pumpType AND pumpId = :pumpId AND pumpSerial = :pumpSerial AND startId IS NOT NULL AND endId IS NULL")
    abstract fun findByPumpId_StartIdIsNotNull_EndIdIsNull(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long?): ExtendedBolus?
}