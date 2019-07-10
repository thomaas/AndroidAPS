package info.nightscout.androidaps.plugins.source

import android.content.Intent
import info.nightscout.androidaps.R
import info.nightscout.androidaps.database.BlockingAppRepository
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.db.BgReading
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.utils.toTrendArrow
import org.json.JSONArray
import org.json.JSONException
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Created by mike on 05.08.2016.
 */
object SourceMM640gPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.MM640g)
        .description(R.string.description_source_mm640g)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    override fun advancedFilteringSupported(): Boolean {
        return false
    }

    override fun handleNewData(intent: Intent) {

        if (!isEnabled(PluginType.BGSOURCE)) return

        val bundle = intent.extras ?: return

        val collection = bundle.getString("collection") ?: return

        if (collection == "entries") {
            val data = bundle.getString("data")
            if (L.isEnabled(L.BGSOURCE))
                log.debug("Received MM640g Data: ", data)

            if (data != null && data.isNotEmpty()) {
                try {
                    val jsonArray = JSONArray(data)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        when (val type = jsonObject.getString("type")) {
                            "sgv" -> {
                                val bgReading = BgReading()
                                val timestamp = jsonObject.getLong("date")
                                val glucoseValue = GlucoseValue(
                                        utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                                        timestamp = timestamp,
                                        value = jsonObject.getDouble("sgv"),
                                        trendArrow = jsonObject.getString("direction").toTrendArrow(),
                                        raw = null,
                                        noise = null,
                                        sourceSensor = GlucoseValue.SourceSensor.MM_600_SERIES
                                )

                                BlockingAppRepository.createOrUpdateBasedOnTimestamp(glucoseValue)
                            }
                            else -> if (L.isEnabled(L.BGSOURCE))
                                log.debug("Unknown entries type: $type")
                        }
                    }
                } catch (e: JSONException) {
                    log.error("Exception: ", e)
                }

            }
        }
    }
}
