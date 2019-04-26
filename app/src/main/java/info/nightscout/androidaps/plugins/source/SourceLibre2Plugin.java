package info.nightscout.androidaps.plugins.source;

import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.interfaces.BgSourceInterface;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PluginDescription;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload;
import info.nightscout.androidaps.utils.SP;

public class SourceLibre2Plugin extends PluginBase implements BgSourceInterface {

    private static Logger log = LoggerFactory.getLogger(L.BGSOURCE);

    private static SourceLibre2Plugin plugin = null;

    public static SourceLibre2Plugin getPlugin() {
        if (plugin == null) plugin = new SourceLibre2Plugin();
        return plugin;
    }

    private SourceLibre2Plugin() {
        super(new PluginDescription()
                .mainType(PluginType.BGSOURCE)
                .fragmentClass(BGSourceFragment.class.getName())
                .pluginName(R.string.libre2_app)
                .shortName(R.string.libre2_short)
                .description(R.string.libre2_description));
    }

    @Override
    public boolean advancedFilteringSupported() {
        return true;
    }

    @Override
    public void handleNewData(Intent intent) {
        if (!isEnabled(PluginType.BGSOURCE)) return;
        if (intent.hasExtra("glucose") && intent.hasExtra("timestamp")) {
            double glucose = intent.getDoubleExtra("glucose", 0);
            long timestamp = intent.getLongExtra("timestamp", 0);
            log.debug("Received BG reading from LibreLink: glucose=" + glucose + " timestamp=" + timestamp);
            BgReading bgReading = new BgReading();
            bgReading.value = glucose;
            bgReading.date = timestamp;
            bgReading.raw = 0;
            BgReading bgReadingBefore = MainApp.getDbHelper().getBgReadingBefore(timestamp);
            long timeDifference = timestamp - bgReadingBefore.date;
            if (timeDifference <= 20 * 60 * 1000) {
                double slope = (glucose - bgReadingBefore.value) / (double) (timeDifference / (60 * 1000));
                if (slope <= -3.5) bgReading.direction = "DoubleDown";
                else if (slope <= -2) bgReading.direction = "SingleDown";
                else if (slope <= -1) bgReading.direction = "FortyFiveDown";
                else if (slope <= 1) bgReading.direction = "Flat";
                else if (slope <= 2) bgReading.direction = "FortyFiveUp";
                else if (slope <= 3.5) bgReading.direction = "SingleUp";
                else bgReading.direction = "DoubleUp";
            } else bgReading.direction = "NONE";
            MainApp.getDbHelper().createIfNotExists(bgReading, "Libre2");
            if (SP.getBoolean(R.string.key_dexcomg5_nsupload, false))
                NSUpload.uploadBg(bgReading, "AndroidAPS-Libre2");

            if (SP.getBoolean(R.string.key_dexcomg5_xdripupload, false))
                NSUpload.sendToXdrip(bgReading);
        } else {
            log.error("Received faulty intent from LibreLink.");
        }
    }
}
