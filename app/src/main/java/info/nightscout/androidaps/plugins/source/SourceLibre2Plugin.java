package info.nightscout.androidaps.plugins.source;

import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.db.CareportalEvent;
import info.nightscout.androidaps.interfaces.BgSourceInterface;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PluginDescription;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload;
import info.nightscout.androidaps.services.Intents;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.utils.SP;

public class SourceLibre2Plugin extends PluginBase implements BgSourceInterface {

    private static long SMOOTHING_DURATION = TimeUnit.MINUTES.toMillis(7);
    private static long TREND_DURATION = TimeUnit.MINUTES.toMillis(10);

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
                .preferencesId(R.xml.pref_bgsource_libre2)
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
        if (Intents.LIBRE2_ACTIVATION.equals(intent.getAction()))
            saveSensorStartTime(intent.getBundleExtra("sensor"));
        if (Intents.LIBRE2_BG.equals(intent.getAction())) {
            Bundle sas = intent.getBundleExtra("sas");
            if (sas != null) saveSensorStartTime(sas.getBundle("currentSensor"));
            if (!intent.hasExtra("glucose") || !intent.hasExtra("timestamp") || !intent.hasExtra("bleManager")) {
                log.error("Received faulty intent from LibreLink.");
                return;
            }
            double glucose = intent.getDoubleExtra("glucose", 0);
            long timestamp = intent.getLongExtra("timestamp", 0);
            String serial = intent.getBundleExtra("bleManager").getString("sensorSerial");
            if (serial == null) {
                log.error("Received faulty intent from LibreLink.");
                return;
            }
            log.debug("Received BG reading from LibreLink: glucose=" + glucose + " timestamp=" + timestamp + " serial=" + serial);

            Libre2RawValue currentRawValue = new Libre2RawValue();
            currentRawValue.timestamp = timestamp;
            currentRawValue.glucose = glucose;
            currentRawValue.serial = serial;

            List<Libre2RawValue> smoothingValues = MainApp.getDbHelper().getLibre2RawValuesBetween(serial, timestamp - SMOOTHING_DURATION, timestamp);
            List<Libre2RawValue> trendValues = MainApp.getDbHelper().getLibre2RawValuesBetween(serial, timestamp - TREND_DURATION, timestamp);
            smoothingValues.add(currentRawValue);
            trendValues.add(currentRawValue);
            MainApp.getDbHelper().createOrUpdate(currentRawValue);

            BgReading bgReading = new BgReading();
            bgReading.raw = currentRawValue.glucose;
            bgReading.date = currentRawValue.timestamp;
            bgReading.value = calculateAverageValue(smoothingValues);
            bgReading.direction = calculateTrend(trendValues);

            MainApp.getDbHelper().createIfNotExists(bgReading, "Libre2");

            if (SP.getBoolean(R.string.key_dexcomg5_nsupload, false))
                NSUpload.uploadBg(bgReading, "AndroidAPS-Libre2");

            if (SP.getBoolean(R.string.key_dexcomg5_xdripupload, false))
                NSUpload.sendToXdrip(bgReading);
        }
    }

    private static void saveSensorStartTime(Bundle sensor) {
        if (sensor != null && sensor.containsKey("sensorStartTime")) {
            long sensorStartTime = sensor.getLong("sensorStartTime");
            if (MainApp.getDbHelper().getCareportalEventFromTimestamp(sensorStartTime) == null) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("enteredBy", "AndroidAPS-Libre2");
                    data.put("created_at", DateUtil.toISOString(sensorStartTime));
                    data.put("eventType", CareportalEvent.SENSORCHANGE);
                    NSUpload.uploadCareportalEntryToNS(data);
                } catch (JSONException e) {
                    log.error("Exception in Libre 2 plugin", e);
                }
            }
        }
    }

    private static double calculateAverageValue(List<Libre2RawValue> rawValues) {
        double sum = 0;
        for (Libre2RawValue rawValue : rawValues) {
            sum += rawValue.glucose;
        }
        return Math.round(sum / (double) rawValues.size());
    }

    private static String calculateTrend(List<Libre2RawValue> rawValues) {
        if (rawValues.size() <= 1) return "NONE";
        Collections.sort(rawValues, (o1, o2) -> Long.compare(o1.timestamp, o2.timestamp));

        long oldestTimestamp = rawValues.get(0).timestamp;
        double sumX = 0;
        double sumY = 0;
        for (Libre2RawValue value : rawValues) {
            sumX += (double) (value.timestamp - oldestTimestamp) / (double) TimeUnit.MINUTES.toMillis(1);
            sumY += value.glucose;
        }
        double averageGlucose = sumY / rawValues.size();
        double averageTimestamp = sumX / rawValues.size();
        double a = 0;
        double b = 0;
        for (Libre2RawValue value : rawValues) {
            a += ((double) (value.timestamp - oldestTimestamp) / (double) TimeUnit.MINUTES.toMillis(1) - averageTimestamp) * (value.glucose - averageGlucose);
            b += Math.pow((double) (value.timestamp - oldestTimestamp) / (double) TimeUnit.MINUTES.toMillis(1) - averageTimestamp, 2);
        }
        double slope = a / b;
        return determineTrendArrow(slope);
    }

    private static String determineTrendArrow(double slope) {
        if (slope <= -3.5) return "DoubleDown";
        else if (slope <= -2) return "SingleDown";
        else if (slope <= -1) return "FortyFiveDown";
        else if (slope <= 1) return "Flat";
        else if (slope <= 2) return "FortyFiveUp";
        else if (slope <= 3.5) return "SingleUp";
        else return "DoubleUp";
    }
}
