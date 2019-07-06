package info.nightscout.androidaps.database.entities

import androidx.room.*
import info.nightscout.androidaps.database.TABLE_TOTAL_DAILY_DOSE
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime

@Entity(tableName = TABLE_TOTAL_DAILY_DOSE,
        foreignKeys = [ForeignKey(
                entity = TotalDailyDose::class,
                parentColumns = ["id"],
                childColumns = ["referenceID"])],
        indices = [Index("referenceID"), Index("timestamp")])
data class TotalDailyDose(
        @PrimaryKey(autoGenerate = true)
        override var id: Long = 0,
        override var version: Int = 0,
        override var lastModified: Long = -1,
        override var valid: Boolean = true,
        override var referenceID: Long = 0,
        @Embedded
        override var interfaceIDs: InterfaceIDs = InterfaceIDs(),
        override var timestamp: Long,
        override var utcOffset: Long,
        var basalAmount: Double?,
        var totalAmount: Double?,
        var bolusAmount: Double?
) : DBEntry, DBEntryWithTime