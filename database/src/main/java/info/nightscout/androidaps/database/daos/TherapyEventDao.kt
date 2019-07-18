package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_THERAPY_EVENTS
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.TherapyEvent

@Dao
abstract class TherapyEventDao : BaseDao<TherapyEvent>() {

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE id = :id")
    abstract override fun findById(id: Long): TherapyEvent?

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE type IN ('PUMP_STARTED', 'PUMP_STOPPED', 'PUMP_PAUSED') AND pumpId < :pumpId AND pumpSerial = :pumpSerial AND pumpType = :pumpType AND valid = 1 AND referenceId IS NULL ORDER BY timestamp DESC LIMIT 1")
    abstract fun getOperatingModeEventForPumpWithSmallerPumpId(pumpType: InterfaceIDs.PumpType, pumpSerial: String, pumpId: Long): TherapyEvent?

    @Query("SELECT * FROM $TABLE_THERAPY_EVENTS WHERE type = :type AND timestamp = :timestamp")
    abstract fun findByTimestamp(type: TherapyEvent.Type, timestamp: Long): TherapyEvent?

}