package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.database.entities.TherapyEvent
import java.util.*

class GlucoseValuesTransaction(
        private val glucoseValues: List<GlucoseValue>,
        private val calibrations: List<Calibration>,
        private val sensorInsertionTime: Long?
) : Transaction<List<GlucoseValue>>() {

    override fun run(): List<info.nightscout.androidaps.database.entities.GlucoseValue> {
        val insertedGlucoseValues = mutableListOf<info.nightscout.androidaps.database.entities.GlucoseValue>()
        glucoseValues.forEach {
            val current = AppRepository.database.glucoseValueDao.findByTimestamp(it.timestamp)
            val glucoseValue = GlucoseValue(
                    utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                    timestamp = it.timestamp,
                    raw = it.raw,
                    value = it.value,
                    noise = it.noise,
                    trendArrow = it.trendArrow,
                    sourceSensor = it.sourceSensor
            )
            glucoseValue.interfaceIDs.nightscoutId = it.nightscoutId
            when {
                current == null -> {
                    AppRepository.database.glucoseValueDao.insertNewEntry(glucoseValue)
                    inserted.add(glucoseValue)
                    insertedGlucoseValues.add(glucoseValue)
                }
                current.contentEqualsTo(glucoseValue) -> return@forEach
                else -> {
                    glucoseValue.id = current.id
                    AppRepository.database.glucoseValueDao.updateExistingEntry(glucoseValue)
                    updated.add(glucoseValue)
                }
            }
        }
        calibrations.forEach {
            if (AppRepository.database.therapyEventDao.findByTimestamp(TherapyEvent.Type.FINGER_STICK_BG_VALUE, it.timestamp) == null) {
                AppRepository.database.therapyEventDao.insertNewEntry(TherapyEvent(
                        timestamp = it.timestamp,
                        utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                        type = TherapyEvent.Type.FINGER_STICK_BG_VALUE,
                        amount = it.value
                ).apply { inserted.add(this) })
            }
        }
        sensorInsertionTime?.let {
            if (AppRepository.database.therapyEventDao.findByTimestamp(TherapyEvent.Type.SENSOR_INSERTED, it) == null) {
                AppRepository.database.therapyEventDao.insertNewEntry(TherapyEvent(
                        timestamp = it,
                        utcOffset = TimeZone.getDefault().getOffset(it).toLong(),
                        type = TherapyEvent.Type.SENSOR_INSERTED
                ).apply { inserted.add(this) })
            }
        }
        return insertedGlucoseValues
    }

    data class GlucoseValue(
            val timestamp: Long,
            val value: Double,
            val raw: Double?,
            val noise: Double?,
            val trendArrow: info.nightscout.androidaps.database.entities.GlucoseValue.TrendArrow,
            val sourceSensor: info.nightscout.androidaps.database.entities.GlucoseValue.SourceSensor,
            val nightscoutId: String? = null
    )

    data class Calibration(
            val timestamp: Long,
            val value: Double
    )
}