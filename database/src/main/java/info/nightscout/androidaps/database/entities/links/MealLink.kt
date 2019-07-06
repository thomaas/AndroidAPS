package info.nightscout.androidaps.database.entities.links

import androidx.room.*
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.TABLE_MEAL_LINKS
import info.nightscout.androidaps.database.entities.*

@Entity(tableName = TABLE_MEAL_LINKS,
        foreignKeys = [ForeignKey(
                entity = Bolus::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("bolusID")), ForeignKey(

                entity = Carbs::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("carbsID")), ForeignKey(

                entity = BolusCalculatorResult::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("bolusCalcResultID")), ForeignKey(

                entity = TemporaryBasal::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("superbolusTempBasalID")), ForeignKey(

                entity = TherapyEvent::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("noteID")), ForeignKey(

                entity = MealLink::class,
                parentColumns = ["id"],
                childColumns = ["referenceID"])],
        indices = [Index("referenceID"), Index("bolusID"),
                Index("carbsID"), Index("bolusCalcResultID"),
                Index("superbolusTempBasalID"), Index("noteID")])
data class MealLink(
        @PrimaryKey(autoGenerate = true)
        override var id: Long = 0,
        override var version: Int = 0,
        override var lastModified: Long = -1,
        override var valid: Boolean = true,
        override var referenceID: Long? = null,
        @Embedded
        override var interfaceIDs2: InterfaceIDs? = null,
        var bolusID: Long? = null,
        var carbsID: Long? = null,
        var bolusCalcResultID: Long? = null,
        var superbolusTempBasalID: Long? = null,
        var noteID: Long? = null
) : DBEntry {
    override val foreignKeysValid: Boolean
        get() = super.foreignKeysValid && bolusID != 0L && carbsID != 0L &&
                bolusCalcResultID != 0L && superbolusTempBasalID != 0L && noteID != 0L
}