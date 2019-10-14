package info.nightscout.androidaps.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import info.nightscout.androidaps.database.daos.*
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.entities.links.APSResultLink
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink

@Database(version = 1, entities = arrayOf(APSResult::class, Bolus::class, BolusCalculatorResult::class, Carbs::class,
        EffectiveProfileSwitch::class, ExtendedBolus::class, GlucoseValue::class, ProfileSwitch::class,
        TemporaryBasal::class, TemporaryTarget::class, TherapyEvent::class, TotalDailyDose::class,
        APSResultLink::class, MealLink::class, MultiwaveBolusLink::class))
@TypeConverters(Converters::class)
internal abstract class AppDatabase : RoomDatabase() {

    val daos = lazy {
        arrayOf(glucoseValueDao, therapyEventDao, temporaryBasalDao, bolusDao, extendedBolusDao,
                multiwaveBolusLinkDao, totalDailyDoseDao, carbsDao, mealLinkDao, temporaryTargetDao,
                apsResultLinkDao, bolusCalculatorResultDao, effectiveProfileSwitchDao, profileSwitchDao, apsResultDao)
    }

    abstract val glucoseValueDao: GlucoseValueDao

    abstract val therapyEventDao: TherapyEventDao

    abstract val temporaryBasalDao: TemporaryBasalDao

    abstract val bolusDao: BolusDao

    abstract val extendedBolusDao: ExtendedBolusDao

    abstract val multiwaveBolusLinkDao: MultiwaveBolusLinkDao

    abstract val totalDailyDoseDao: TotalDailyDoseDao

    abstract val carbsDao: CarbsDao

    abstract val mealLinkDao: MealLinkDao

    abstract val temporaryTargetDao: TemporaryTargetDao

    abstract val apsResultLinkDao: APSResultLinkDao

    abstract val bolusCalculatorResultDao: BolusCalculatorResultDao

    abstract val effectiveProfileSwitchDao: EffectiveProfileSwitchDao

    abstract val profileSwitchDao: ProfileSwitchDao

    abstract val apsResultDao: APSResultDao

}