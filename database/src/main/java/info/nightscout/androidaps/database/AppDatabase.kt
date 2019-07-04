package info.nightscout.androidaps.database

import androidx.room.Database
import androidx.room.DatabaseView
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import info.nightscout.androidaps.database.daos.GlucoseValueDao
import info.nightscout.androidaps.database.entities.*

@Database(version = 1, entities = arrayOf(APSResult::class, Bolus::class, BolusCalculatorResult::class, Carbs::class,
        EffectiveProfileSwitch::class, ExtendedBolus::class, GlucoseValue::class, ProfileSwitch::class,
        TemporaryBasal::class, TemporaryTarget::class, TherapyEvent::class, TotalDailyDose::class))
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun glucoseValueDao() : GlucoseValueDao

}