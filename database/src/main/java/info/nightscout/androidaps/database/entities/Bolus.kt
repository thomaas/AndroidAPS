package info.nightscout.androidaps.database.entities

import androidx.room.*
import info.nightscout.androidaps.database.TABLE_BOLUSES
import info.nightscout.androidaps.database.embedments.InsulinConfiguration
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.DBEntryWithInsulinConfig
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime

@Entity(tableName = TABLE_BOLUSES,
        foreignKeys = [ForeignKey(
                entity = Bolus::class,
                parentColumns = ["id"],
                childColumns = ["referenceID"])],
        indices = [Index("referenceID"), Index("timestamp")])
data class Bolus(
        @PrimaryKey(autoGenerate = true)
        override var id: Long = 0,
        override var version: Int = 0,
        override var lastModified: Long = -1,
        override var valid: Boolean = true,
        override var referenceID: Long? = null,
        @Embedded
        override var interfaceIDs2: InterfaceIDs? = null,
        override var timestamp: Long,
        override var utcOffset: Long,
        var amount: Double,
        var type: Type,
        var basalInsulin: Boolean,
        @Embedded
        override var insulinConfiguration: InsulinConfiguration
) : DBEntry<Bolus>, DBEntryWithTime, DBEntryWithInsulinConfig {
    enum class Type {
        NORMAL,
        SMB,
        PRIMING
    }
}