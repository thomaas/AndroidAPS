package info.nightscout.androidaps.plugins.source

import android.content.Intent
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.GlucoseValue
import info.nightscout.androidaps.db.BgReading
import info.nightscout.androidaps.interfaces.BgSourceInterface
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.logging.BundleLogger
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.services.Intents
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Created by mike on 05.08.2016.
 */
object SourceXdripPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.BGSOURCE)
        .fragmentClass(BGSourceFragment::class.java.name)
        .pluginName(R.string.xdrip)
        .description(R.string.description_source_xdrip)), BgSourceInterface {

    private val log = LoggerFactory.getLogger(L.BGSOURCE)

    private var advancedFiltering: Boolean = false

    override fun advancedFilteringSupported(): Boolean {
        return advancedFiltering
    }

    override fun handleNewData(intent: Intent) {
        if (!isEnabled(PluginType.BGSOURCE)) return

        try {
            val bundle = intent.extras!!

            if (L.isEnabled(L.BGSOURCE))
                log.debug("Received xDrip data: " + BundleLogger.log(bundle))

            val bgReading = BgReading()

            bgReading.value = bundle.getDouble(Intents.EXTRA_BG_ESTIMATE)
            bgReading.direction = bundle.getString(Intents.EXTRA_BG_SLOPE_NAME)
            bgReading.date = bundle.getLong(Intents.EXTRA_TIMESTAMP)
            bgReading.raw = bundle.getDouble(Intents.EXTRA_RAW)
            val source = bundle.getString(Intents.XDRIP_DATA_SOURCE_DESCRIPTION, "no Source specified")
            this.advancedFiltering = source.contains("G5 Native") || source.contains("G6 Native")
            MainApp.getDbHelper().createIfNotExists(bgReading, "XDRIP")

            val trendArrow: GlucoseValue.TrendArrow
            trendArrow = try {
                GlucoseValue.TrendArrow.valueOf(bundle.getString(Intents.EXTRA_BG_SLOPE_NAME)!!.toUpperCase())
            } catch (e: IllegalArgumentException) {
                GlucoseValue.TrendArrow.NONE
            }

            val timestamp = bundle.getLong(Intents.EXTRA_TIMESTAMP)

            val glucoseValue = GlucoseValue(
                    value = bundle.getDouble(Intents.EXTRA_BG_ESTIMATE),
                    trendArrow = trendArrow,
                    timestamp = timestamp,
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    raw = bundle.getDouble(Intents.EXTRA_RAW),
                    sourceSensor = GlucoseValue.SourceSensor.UNKNOWN,
                    noise = null
            )
            log.debug("TrendArrow: " + bundle.getString(Intents.EXTRA_BG_SLOPE_NAME))
            AppRepository.createOrUpdateBasedOnTimestamp(glucoseValue)
                    .observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .subscribe()
        } catch (e: Throwable) {
            log.error("Error while processing intent", e)
        }
    }
}
