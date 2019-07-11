package info.nightscout.androidaps.plugins.source

import android.content.Intent
import info.nightscout.androidaps.R
import info.nightscout.androidaps.database.BlockingAppRepository
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.plugins.constraints.objectives.ObjectivesPlugin
import info.nightscout.androidaps.plugins.general.nsclient.data.NSSgv
import info.nightscout.androidaps.utils.JsonHelper
import info.nightscout.androidaps.utils.SP
import info.nightscout.androidaps.utils.determineSourceSensor
import info.nightscout.androidaps.utils.toTrendArrow
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Created by mike on 05.08.2016.
 */
object SourceNSClientPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.nsclientbg)
        .description(R.string.description_source_ns_client)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    private var lastBGTimeStamp: Long = 0
    private var isAdvancedFilteringEnabled = false

    override fun advancedFilteringSupported(): Boolean {
        return isAdvancedFilteringEnabled
    }

    override fun handleNewData(intent: Intent) {

        if (!isEnabled(PluginType.BGSOURCE) && !SP.getBoolean(R.string.key_ns_autobackfill, true))
            return

        val bundles = intent.extras

        try {
            if (bundles!!.containsKey("sgv")) {
                val sgvstring = bundles.getString("sgv")
                if (L.isEnabled(L.BGSOURCE))
                    log.debug("Received NS Data: " + sgvstring!!)

                val sgvJson = JSONObject(sgvstring)
                storeSgv(sgvJson)
            }

            if (bundles.containsKey("sgvs")) {
                val sgvstring = bundles.getString("sgvs")
                if (L.isEnabled(L.BGSOURCE))
                    log.debug("Received NS Data: " + sgvstring!!)
                val jsonArray = JSONArray(sgvstring)
                for (i in 0 until jsonArray.length()) {
                    val sgvJson = jsonArray.getJSONObject(i)
                    storeSgv(sgvJson)
                }
            }
        } catch (e: Exception) {
            log.error("Unhandled exception", e)
        }

        // Objectives 0
        ObjectivesPlugin.getPlugin().bgIsAvailableInNS = true
        ObjectivesPlugin.getPlugin().saveProgress()
    }

    private fun storeSgv(sgvJson: JSONObject) {
        val nsSgv = NSSgv(sgvJson)

        val source = JsonHelper.safeGetString(sgvJson, "device", "none")

        val glucoseValue = GlucoseValue(
                utcOffset = TimeZone.getDefault().getOffset(nsSgv.mills).toLong(),
                timestamp = nsSgv.mills,
                value = nsSgv.mgdl.toDouble(),
                trendArrow = nsSgv.direction.toTrendArrow(),
                raw = nsSgv.filtered?.toDouble(),
                noise = nsSgv.noise?.toDouble(),
                sourceSensor = source.determineSourceSensor()
        )
        glucoseValue.interfaceIDs.nightscoutId = nsSgv.id

        BlockingAppRepository.createOrUpdateBasedOnTimestamp(glucoseValue)
        detectSource(source, JsonHelper.safeGetLong(sgvJson, "mills"))
    }

    private fun detectSource(source: String, timeStamp: Long) {
        if (timeStamp > lastBGTimeStamp) {
            isAdvancedFilteringEnabled = source.contains("G5 Native") || source.contains("G6 Native") || source.contains("AndroidAPS-Dexcom")
            lastBGTimeStamp = timeStamp
        }
    }
}
