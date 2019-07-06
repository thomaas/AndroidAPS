package info.nightscout.androidaps.database.entities.links

import androidx.room.*
import info.nightscout.androidaps.database.TABLE_APS_RESULT_LINKS
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.APSResult
import info.nightscout.androidaps.database.entities.Bolus
import info.nightscout.androidaps.database.entities.TemporaryBasal
import info.nightscout.androidaps.database.interfaces.DBEntry

@Entity(tableName = TABLE_APS_RESULT_LINKS,
        foreignKeys = [ForeignKey(
                entity = APSResult::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("apsResultID")), ForeignKey(

                entity = Bolus::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("smbID")), ForeignKey(

                entity = TemporaryBasal::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("tbrID")), ForeignKey(

                entity = APSResultLink::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("referenceID"))],
        indices = [Index("referenceID"), Index("apsResultID"),
                Index("smbID"), Index("tbrID")])
data class APSResultLink(
        @PrimaryKey(autoGenerate = true)
        override var id: Long = 0,
        override var version: Int = 0,
        override var lastModified: Long = -1,
        override var valid: Boolean = true,
        override var referenceID: Long = 0,
        @Embedded
        override var interfaceIDs: InterfaceIDs = InterfaceIDs(),
        var apsResultID: Long = 0,
        var smbID: Long = 0,
        var tbrID: Long= 0
) : DBEntry