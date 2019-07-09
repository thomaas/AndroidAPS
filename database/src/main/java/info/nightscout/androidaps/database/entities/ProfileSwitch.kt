package info.nightscout.androidaps.database.entities

import androidx.room.*
import info.nightscout.androidaps.database.Block
import info.nightscout.androidaps.database.TABLE_PROFILE_SWITCHES
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.DBEntryWithTimeAndDuration

@Entity(tableName = TABLE_PROFILE_SWITCHES,
        foreignKeys = [ForeignKey(
                entity = ProfileSwitch::class,
                parentColumns = ["id"],
                childColumns = ["referenceID"])],
        indices = [Index("referenceID"), Index("timestamp")])
data class ProfileSwitch(
        @PrimaryKey(autoGenerate = true)
        override var id: Long = 0,
        override var version: Int = 0,
        override var lastModified: Long = -1,
        override var valid: Boolean = true,
        override var referenceID: Long? = null,
        @Embedded
        override var interfaceIDs2: InterfaceIDs? = InterfaceIDs(),
        override var timestamp: Long,
        override var utcOffset: Long,
        var profileName: String,
        var glucoseUnit: GlucoseUnit,
        var basalBlocks: List<Block>,
        var isfBlocks: List<Block>,
        var icBlocks: List<Block>,
        var targetBlocks: List<Block>,
        var timeshift: Int,
        var percentage: Int,
        override var duration: Long
) : DBEntry<ProfileSwitch>, DBEntryWithTimeAndDuration {
    enum class GlucoseUnit {
        MGDL,
        MMOL
    }
}