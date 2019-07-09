package info.nightscout.androidaps.plugins.source

import android.content.Intent
import android.os.Bundle
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.database.BlockingAppRepository
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.db.BgReading
import info.nightscout.androidaps.db.CareportalEvent
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload
import info.nightscout.androidaps.services.Intents
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.SP
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

object SourceLibre2Plugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .preferencesId(R.xml.pref_bgsource_libre2)
        .pluginName(R.string.libre2_app)
        .shortName(R.string.libre2_short)
        .description(R.string.libre2_description)), BgSourceInterface {

    override fun advancedFilteringSupported(): Boolean {
        return true
    }

    override fun handleNewData(intent: Intent) {
        if (!isEnabled(PluginType.BGSOURCE)) return
        if (Intents.LIBRE2_ACTIVATION == intent.action)
            saveSensorStartTime(intent.getBundleExtra("sensor"))
        if (Intents.LIBRE2_BG == intent.action) {
            val currentRawValue = processIntent(intent) ?: return
            val lastBG = MainApp.getDbHelper().getBgReadingBefore(currentRawValue.timestamp)
            if (lastBG == null || currentRawValue.timestamp - lastBG.date > TimeUnit.SECONDS.toMillis(270)) {
                val smoothingValues = getValuesSince(currentRawValue, SMOOTHING_DURATION)
                val trendValues = getValuesSince(currentRawValue, TREND_DURATION)
                smoothingValues!!.add(currentRawValue)
                trendValues!!.add(currentRawValue)
                processValues(currentRawValue, smoothingValues, trendValues)
            }
            MainApp.getDbHelper().createOrUpdate(currentRawValue)
        }
    }

    private fun getValuesSince(currentRawValue: Libre2RawValue, smoothingDuration: Long): MutableList<Libre2RawValue>? {
        return MainApp.getDbHelper().getLibre2RawValuesBetween(currentRawValue.serial,
                currentRawValue.timestamp - smoothingDuration, currentRawValue.timestamp)
    }

    private val SMOOTHING_DURATION = TimeUnit.MINUTES.toMillis(20)
    private val TREND_DURATION = TimeUnit.MINUTES.toMillis(10)

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    private fun processValues(currentValue: Libre2RawValue, smoothingValues: List<Libre2RawValue>, trendValues: List<Libre2RawValue>) {
        val bgReading = BgReading()
        bgReading.date = currentValue.timestamp
        bgReading.raw = currentValue.glucose
        bgReading.value = calculateWeightedAverage(smoothingValues, currentValue.timestamp)
        bgReading.direction = calculateTrend(trendValues)
        val trendArrow = when(bgReading.direction) {
            "DoubleDown" -> GlucoseValue.TrendArrow.DOUBLE_DOWN
            "SingleDown" -> GlucoseValue.TrendArrow.SINGLE_DOWN
            "FortyFiveDown" -> GlucoseValue.TrendArrow.FORTY_FIVE_DOWN
            "Flat" -> GlucoseValue.TrendArrow.FLAT
            "FortyFiveUp" -> GlucoseValue.TrendArrow.FORTY_FIVE_UP
            "SingleUp" -> GlucoseValue.TrendArrow.SINGLE_UP
            "DoubleUp" -> GlucoseValue.TrendArrow.DOUBLE_UP
            else -> GlucoseValue.TrendArrow.NONE
        }
        val glucoseValue = GlucoseValue(
                timestamp = currentValue.timestamp,
                utcOffset = TimeZone.getDefault().getOffset(currentValue.timestamp).toLong(),
                trendArrow = trendArrow,
                raw = currentValue.glucose,
                noise = null,
                sourceSensor = GlucoseValue.SourceSensor.LIBRE_2_NATIVE,
                value = bgReading.value
        )
        BlockingAppRepository.createOrUpdateBasedOnTimestamp(glucoseValue)
        MainApp.getDbHelper().createIfNotExists(bgReading, "Libre2")
        if (SP.getBoolean(R.string.key_dexcomg5_nsupload, false))
            NSUpload.uploadBg(bgReading, "AndroidAPS-Libre2")
        if (SP.getBoolean(R.string.key_dexcomg5_xdripupload, false))
            NSUpload.sendToXdrip(bgReading)
    }

    private fun processIntent(intent: Intent): Libre2RawValue? {
        val sas = intent.getBundleExtra("sas")
        if (sas != null) saveSensorStartTime(sas.getBundle("currentSensor"))
        if (!intent.hasExtra("glucose") || !intent.hasExtra("timestamp") || !intent.hasExtra("bleManager")) {
            log.error("Received faulty intent from LibreLink.")
            return null
        }
        val glucose = intent.getDoubleExtra("glucose", 0.0)
        val timestamp = intent.getLongExtra("timestamp", 0)
        val serial = intent.getBundleExtra("bleManager").getString("sensorSerial")
        if (serial == null) {
            log.error("Received faulty intent from LibreLink.")
            return null
        }
        log.debug("Received BG reading from LibreLink: glucose=$glucose timestamp=$timestamp serial=$serial")

        val rawValue = Libre2RawValue()
        rawValue.timestamp = timestamp
        rawValue.glucose = glucose
        rawValue.serial = serial
        return rawValue
    }

    private fun saveSensorStartTime(sensor: Bundle?) {
        if (sensor != null && sensor.containsKey("sensorStartTime")) {
            val sensorStartTime = sensor.getLong("sensorStartTime")
            if (MainApp.getDbHelper().getCareportalEventFromTimestamp(sensorStartTime) == null) {
                try {
                    val data = JSONObject()
                    data.put("enteredBy", "AndroidAPS-Libre2")
                    data.put("created_at", DateUtil.toISOString(sensorStartTime))
                    data.put("eventType", CareportalEvent.SENSORCHANGE)
                    NSUpload.uploadCareportalEntryToNS(data)
                } catch (e: JSONException) {
                    log.error("Exception in Libre 2 plugin", e)
                }

            }
        }
    }

    private fun calculateWeightedAverage(rawValues: List<Libre2RawValue>, now: Long): Double {
        var sum = 0.0
        var weightSum = 0.0
        for (rawValue in rawValues) {
            val weight = 1 - (now - rawValue.timestamp) / SMOOTHING_DURATION.toDouble()
            sum += rawValue.glucose * weight
            weightSum += weight
        }
        return Math.round(sum / weightSum).toDouble()
    }

    private fun calculateTrend(rawValues: List<Libre2RawValue>): String {
        if (rawValues.size <= 1) return "NONE"
        Collections.sort(rawValues) { o1, o2 -> java.lang.Long.compare(o1.timestamp, o2.timestamp) }

        val oldestTimestamp = rawValues[0].timestamp
        var sumX = 0.0
        var sumY = 0.0
        for (value in rawValues) {
            sumX += (value.timestamp - oldestTimestamp).toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble()
            sumY += value.glucose
        }
        val averageGlucose = sumY / rawValues.size
        val averageTimestamp = sumX / rawValues.size
        var a = 0.0
        var b = 0.0
        for (value in rawValues) {
            a += ((value.timestamp - oldestTimestamp).toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble() - averageTimestamp) * (value.glucose - averageGlucose)
            b += Math.pow((value.timestamp - oldestTimestamp).toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble() - averageTimestamp, 2.0)
        }
        val slope = a / b
        return determineTrendArrow(slope)
    }

    private fun determineTrendArrow(slope: Double): String {
        return if (slope <= -3.5)
            "DoubleDown"
        else if (slope <= -2)
            "SingleDown"
        else if (slope <= -1)
            "FortyFiveDown"
        else if (slope <= 1)
            "Flat"
        else if (slope <= 2)
            "FortyFiveUp"
        else if (slope <= 3.5)
            "SingleUp"
        else
            "DoubleUp"
    }
}
