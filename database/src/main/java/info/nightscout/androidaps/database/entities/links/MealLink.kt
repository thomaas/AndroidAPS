package info.nightscout.androidaps.database.entities.links

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
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
                childColumns = ["referenceID"])])
data class MealLink(
        @PrimaryKey(autoGenerate = true)
        override var id: Long = 0,
        override var version: Int = 0,
        override var lastModified: Long = -1,
        override var valid: Boolean = true,
        override var referenceID: Long = 0,
        @Embedded
        override var interfaceIDs: InterfaceIDs = InterfaceIDs(),
        var bolusID: Long = 0,
        var carbsID: Long = 0,
        var bolusCalcResultID: Long = 0,
        var superbolusTempBasalID: Long = 0,
        var noteID: Long = 0
) : DBEntry