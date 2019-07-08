package info.nightscout.androidaps.database.entities

import androidx.room.*
import info.nightscout.androidaps.database.TABLE_GLUCOSE_VALUES
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.DBEntryWithTime

@Entity(tableName = TABLE_GLUCOSE_VALUES,
        foreignKeys = [ForeignKey(
                entity = GlucoseValue::class,
                parentColumns = ["id"],
                childColumns = ["referenceID"])],
        indices = [Index("referenceID"), Index("timestamp")])
data class GlucoseValue(
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
        var raw: Double?,
        var value: Double,
        var trendArrow: TrendArrow,
        var noise: Double?,
        var sourceSensor: SourceSensor
) : DBEntry<GlucoseValue>, DBEntryWithTime {

    override fun contentEqualsTo(other: GlucoseValue): Boolean {
        return timestamp == other.timestamp &&
                utcOffset == other.utcOffset &&
                raw == other.raw &&
                value == other.value &&
                trendArrow == other.trendArrow &&
                noise == other.noise &&
                sourceSensor == other.sourceSensor
    }

    enum class TrendArrow {
        NONE,
        TRIPPLE_UP,
        DOUBLE_UP,
        SINGLE_UP,
        FORTY_FIVE_UP,
        FLAT,
        FORTY_FIVE_DOWN,
        SINGLE_DOWN,
        DOUBLE_DOWN,
        TRIPLE_DOWN
    }

    enum class SourceSensor {
        UNKNOWN,
        DEXCOM_G6_NATIVE,
        DEXCOM_G6_XDRIP,
        DEXCOM_G5_NATIVE,
        DEXCOM_G5_XDRIP,
        DEXCOM_G4,
        LIBRE_1_OOP,
        LIBRE_1_XDRIP,
        LIBRE_2_NATIVE,
        MM_600_SERIES;
    }
}