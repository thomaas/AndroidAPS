package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_PROFILE_SWITCHES
import info.nightscout.androidaps.database.entities.ProfileSwitch

@Suppress("FunctionName")
@Dao
abstract class ProfileSwitchDao : BaseDao<ProfileSwitch>() {

    @Query("SELECT * FROM $TABLE_PROFILE_SWITCHES WHERE id = :id")
    abstract override fun findById(id: Long): ProfileSwitch?

    @Query("DELETE FROM $TABLE_PROFILE_SWITCHES")
    abstract override fun deleteAllEntries()
}