package info.nightscout.androidaps.plugins.source

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import info.nightscout.androidaps.Constants
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.activities.RequestDexcomPermissionActivity
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
import info.nightscout.androidaps.utils.toTrendArrow
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

object SourceDexcomPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.dexcom_app_patched)
        .shortName(R.string.dexcom_short)
        .preferencesId(R.xml.pref_bgsource)
        .description(R.string.description_source_dexcom)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    private val PACKAGE_NAMES = arrayOf("com.dexcom.cgm.region1.mgdl", "com.dexcom.cgm.region1.mmol",
            "com.dexcom.cgm.region2.mgdl", "com.dexcom.cgm.region2.mmol",
            "com.dexcom.g6.region1.mmol", "com.dexcom.g6.region2.mgdl",
            "com.dexcom.g6.region3.mgdl", "com.dexcom.g6.region3.mmol")

    const val PERMISSION = "com.dexcom.cgm.EXTERNAL_PERMISSION"

    override fun advancedFilteringSupported(): Boolean {
        return true
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MainApp.instance(), PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(MainApp.instance(), RequestDexcomPermissionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            MainApp.instance().startActivity(intent)
        }
    }

    fun findDexcomPackageName(): String? {
        val packageManager = MainApp.instance().packageManager;
        for (packageInfo in packageManager.getInstalledPackages(0)) {
            if (PACKAGE_NAMES.contains(packageInfo.packageName)) return packageInfo.packageName
        }
        return null
    }

    override fun handleNewData(intent: Intent) {
        if (!isEnabled(PluginType.BGSOURCE)) return
        try {
            val glucoseValues = intent.getBundleExtra("glucoseValues")
            for (i in 0 until glucoseValues.size()) {
                val glucoseValueBundle = glucoseValues.getBundle(i.toString())
                val timestamp = glucoseValueBundle.getLong("timestamp") * 1000
                val glucoseValue = GlucoseValue(
                        timestamp = timestamp,
                        utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                        value = glucoseValueBundle!!.getInt("glucoseValue").toDouble(),
                        noise = null,
                        raw = null,
                        trendArrow = glucoseValueBundle.getString("trendArrow")!!.toTrendArrow(),
                        sourceSensor = GlucoseValue.SourceSensor.DEXCOM_G6_NATIVE
                )
                if (BlockingAppRepository.createOrUpdateBasedOnTimestamp(glucoseValue)) {
                    if (SP.getBoolean(R.string.key_dexcomg5_nsupload, false)) {
                        NSUpload.uploadBg(glucoseValue, "AndroidAPS-DexcomG6")
                    }
                    if (SP.getBoolean(R.string.key_dexcomg5_xdripupload, false)) {
                        NSUpload.sendToXdrip(glucoseValue)
                    }
                }
            }
            val meters = intent.getBundleExtra("meters")
            for (i in 0 until meters.size()) {
                val meter = meters.getBundle(i.toString())
                val timestamp = meter!!.getLong("timestamp") * 1000
                if (MainApp.getDbHelper().getCareportalEventFromTimestamp(timestamp) != null) continue
                val jsonObject = JSONObject()
                jsonObject.put("enteredBy", "AndroidAPS-Dexcom")
                jsonObject.put("created_at", DateUtil.toISOString(timestamp))
                jsonObject.put("eventType", CareportalEvent.BGCHECK)
                jsonObject.put("glucoseType", "Finger")
                jsonObject.put("glucose", meter.getInt("meterValue"))
                jsonObject.put("units", Constants.MGDL)
                NSUpload.uploadCareportalEntryToNS(jsonObject)
            }
            if (SP.getBoolean(R.string.key_dexcom_lognssensorchange, false) && intent.hasExtra("sensorInsertionTime")) {
                val sensorInsertionTime = intent.extras.getLong("sensorInsertionTime") * 1000
                if (MainApp.getDbHelper().getCareportalEventFromTimestamp(sensorInsertionTime) == null) {
                    val jsonObject = JSONObject()
                    jsonObject.put("enteredBy", "AndroidAPS-Dexcom")
                    jsonObject.put("created_at", DateUtil.toISOString(sensorInsertionTime))
                    jsonObject.put("eventType", CareportalEvent.SENSORCHANGE)
                    NSUpload.uploadCareportalEntryToNS(jsonObject)
                }
            }
        } catch (e: Exception) {
            log.error("Error while processing intent from Dexcom App", e)
        }
    }
}
