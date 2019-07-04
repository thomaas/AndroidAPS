package info.nightscout.androidaps.database.entities.links

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import info.nightscout.androidaps.database.TABLE_MULTIWAVE_BOLUS_LINKS
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.Bolus
import info.nightscout.androidaps.database.entities.ExtendedBolus
import info.nightscout.androidaps.database.interfaces.DBEntry

@Entity(tableName = TABLE_MULTIWAVE_BOLUS_LINKS,
        foreignKeys = [ForeignKey(
                entity = Bolus::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("bolusID")), ForeignKey(

                entity = ExtendedBolus::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("extendedBolusID")), ForeignKey(

                entity = MultiwaveBolusLink::class,
                parentColumns = ["id"],
                childColumns = ["referenceID"])])
data class MultiwaveBolusLink(
        @PrimaryKey(autoGenerate = true)
        override var id: Long = 0,
        override var version: Int = 0,
        override var lastModified: Long = -1,
        override var valid: Boolean = true,
        override var referenceID: Long = 0,
        @Embedded
        override var interfaceIDs: InterfaceIDs = InterfaceIDs(),
        var bolusID: Long = 0,
        var extendedBolusID: Long = 0
) : DBEntry