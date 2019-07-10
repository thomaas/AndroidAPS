package info.nightscout.androidaps.plugins.source

import android.content.Intent
import info.nightscout.androidaps.Constants
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.database.BlockingAppRepository
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.db.CareportalEvent
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.SP
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Created by mike on 28.11.2017.
 */

object SourceEversensePlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.eversense)
        .shortName(R.string.eversense_shortname)
        .preferencesId(R.xml.pref_poctech)
        .description(R.string.description_source_eversense)), BgSourceInterface {


    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    override fun advancedFilteringSupported(): Boolean {
        return false
    }

    override fun handleNewData(intent: Intent) {

        if (!isEnabled(PluginType.BGSOURCE)) return

        val bundle = intent.extras ?: return

        if (L.isEnabled(L.BGSOURCE)) {
            if (bundle.containsKey("currentCalibrationPhase"))
                log.debug("currentCalibrationPhase: " + bundle.getString("currentCalibrationPhase")!!)
            if (bundle.containsKey("placementModeInProgress"))
                log.debug("placementModeInProgress: " + bundle.getBoolean("placementModeInProgress"))
            if (bundle.containsKey("glucoseLevel"))
                log.debug("glucoseLevel: " + bundle.getInt("glucoseLevel"))
            if (bundle.containsKey("glucoseTrendDirection"))
                log.debug("glucoseTrendDirection: " + bundle.getString("glucoseTrendDirection")!!)
            if (bundle.containsKey("glucoseTimestamp"))
                log.debug("glucoseTimestamp: " + DateUtil.dateAndTimeFullString(bundle.getLong("glucoseTimestamp")))
            if (bundle.containsKey("batteryLevel"))
                log.debug("batteryLevel: " + bundle.getString("batteryLevel")!!)
            if (bundle.containsKey("signalStrength"))
                log.debug("signalStrength: " + bundle.getString("signalStrength")!!)
            if (bundle.containsKey("transmitterVersionNumber"))
                log.debug("transmitterVersionNumber: " + bundle.getString("transmitterVersionNumber")!!)
            if (bundle.containsKey("isXLVersion"))
                log.debug("isXLVersion: " + bundle.getBoolean("isXLVersion"))
            if (bundle.containsKey("transmitterModelNumber"))
                log.debug("transmitterModelNumber: " + bundle.getString("transmitterModelNumber")!!)
            if (bundle.containsKey("transmitterSerialNumber"))
                log.debug("transmitterSerialNumber: " + bundle.getString("transmitterSerialNumber")!!)
            if (bundle.containsKey("transmitterAddress"))
                log.debug("transmitterAddress: " + bundle.getString("transmitterAddress")!!)
            if (bundle.containsKey("sensorInsertionTimestamp"))
                log.debug("sensorInsertionTimestamp: " + DateUtil.dateAndTimeFullString(bundle.getLong("sensorInsertionTimestamp")))
            if (bundle.containsKey("transmitterVersionNumber"))
                log.debug("transmitterVersionNumber: " + bundle.getString("transmitterVersionNumber")!!)
            if (bundle.containsKey("transmitterConnectionState"))
                log.debug("transmitterConnectionState: " + bundle.getString("transmitterConnectionState")!!)
        }

        if (bundle.containsKey("glucoseLevels")) {
            val glucoseLevels = bundle.getIntArray("glucoseLevels")
            val glucoseRecordNumbers = bundle.getIntArray("glucoseRecordNumbers")
            val glucoseTimestamps = bundle.getLongArray("glucoseTimestamps")

            if (L.isEnabled(L.BGSOURCE)) {
                log.debug("glucoseLevels", Arrays.toString(glucoseLevels))
                log.debug("glucoseRecordNumbers", Arrays.toString(glucoseRecordNumbers))
                log.debug("glucoseTimestamps", Arrays.toString(glucoseTimestamps))
            }

            for (i in glucoseLevels!!.indices) {
                val timestamp = glucoseTimestamps!![i]
                val glucoseValue = GlucoseValue(
                        value = glucoseLevels[i].toDouble(),
                        utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                        timestamp = timestamp,
                        raw = null,
                        sourceSensor = GlucoseValue.SourceSensor.EVERSENSE,
                        noise = null,
                        trendArrow = GlucoseValue.TrendArrow.NONE
                )
                if (BlockingAppRepository.createOrUpdateBasedOnTimestamp(glucoseValue)) {
                    if (SP.getBoolean(R.string.key_dexcomg5_nsupload, false)) {
                        NSUpload.uploadBg(glucoseValue, "AndroidAPS-Eversense")
                    }
                    if (SP.getBoolean(R.string.key_dexcomg5_xdripupload, false)) {
                        NSUpload.sendToXdrip(glucoseValue)
                    }
                }
            }
        }

        if (bundle.containsKey("calibrationGlucoseLevels")) {
            val calibrationGlucoseLevels = bundle.getIntArray("calibrationGlucoseLevels")
            val calibrationTimestamps = bundle.getLongArray("calibrationTimestamps")
            val calibrationRecordNumbers = bundle.getLongArray("calibrationRecordNumbers")

            if (L.isEnabled(L.BGSOURCE)) {
                log.debug("calibrationGlucoseLevels", Arrays.toString(calibrationGlucoseLevels))
                log.debug("calibrationTimestamps", Arrays.toString(calibrationTimestamps))
                log.debug("calibrationRecordNumbers", Arrays.toString(calibrationRecordNumbers))
            }

            for (i in calibrationGlucoseLevels!!.indices) {
                try {
                    if (MainApp.getDbHelper().getCareportalEventFromTimestamp(calibrationTimestamps!![i]) == null) {
                        val data = JSONObject()
                        data.put("enteredBy", "AndroidAPS-Eversense")
                        data.put("created_at", DateUtil.toISOString(calibrationTimestamps[i]))
                        data.put("eventType", CareportalEvent.BGCHECK)
                        data.put("glucoseType", "Finger")
                        data.put("glucose", calibrationGlucoseLevels[i])
                        data.put("units", Constants.MGDL)
                        NSUpload.uploadCareportalEntryToNS(data)
                    }
                } catch (e: JSONException) {
                    log.error("Unhandled exception", e)
                }

            }
        }
    }
}
