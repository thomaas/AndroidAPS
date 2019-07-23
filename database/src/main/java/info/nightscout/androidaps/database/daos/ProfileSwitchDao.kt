package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_APS_RESULT_LINKS
import info.nightscout.androidaps.database.entities.ProfileSwitch

@Suppress("FunctionName")
@Dao
abstract class ProfileSwitchDao : BaseDao<ProfileSwitch>() {

    @Query("SELECT * FROM $TABLE_APS_RESULT_LINKS WHERE id = :id")
    abstract override fun findById(id: Long): ProfileSwitch?

    @Query("DELETE FROM $TABLE_APS_RESULT_LINKS")
    abstract override fun deleteAllEntries()
}