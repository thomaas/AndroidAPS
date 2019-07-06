package info.nightscout.androidaps.database.entities

import androidx.annotation.NonNull
import androidx.room.*
import info.nightscout.androidaps.database.TABLE_CARBS
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.DBEntryWithTimeAndDuration

@Entity(tableName = TABLE_CARBS,
        foreignKeys = [ForeignKey(
                entity = Carbs::class,
                parentColumns = ["id"],
                childColumns = ["referenceID"])],
        indices = [Index("referenceID"), Index("timestamp")])
data class Carbs(
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
        override var duration: Long,
        var amount: Double
) : DBEntry, DBEntryWithTimeAndDuration