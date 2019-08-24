package info.nightscout.androidaps.plugins.general.overview;

import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.data.QuickWizard;
import info.nightscout.androidaps.events.EventRefreshOverview;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PluginDescription;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.bus.RxBus;
import info.nightscout.androidaps.plugins.configBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.general.overview.events.EventDismissNotification;
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification;
import info.nightscout.androidaps.plugins.general.overview.notifications.NotificationStore;
import info.nightscout.androidaps.utils.FabricPrivacy;
import info.nightscout.androidaps.utils.SP;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by mike on 05.08.2016.
 */
public class OverviewPlugin extends PluginBase {
    private static Logger log = LoggerFactory.getLogger(L.OVERVIEW);

    private static OverviewPlugin overviewPlugin = new OverviewPlugin();

    public static OverviewPlugin getPlugin() {
        if (overviewPlugin == null)
            overviewPlugin = new OverviewPlugin();
        return overviewPlugin;
    }

    private CompositeDisposable disposable = new CompositeDisposable();

    public static double bgTargetLow = 80d;
    public static double bgTargetHigh = 180d;

    public QuickWizard quickWizard = new QuickWizard();

    public NotificationStore notificationStore = new NotificationStore();

    public OverviewPlugin() {
        super(new PluginDescription()
                .mainType(PluginType.GENERAL)
                .fragmentClass(OverviewFragment.class.getName())
                .alwaysVisible(true)
                .alwaysEnabled(true)
                .pluginName(R.string.overview)
                .shortName(R.string.overview_shortname)
                .preferencesId(R.xml.pref_overview)
                .description(R.string.description_overview)
        );
        String storedData = SP.getString("QuickWizard", "[]");
        try {
            quickWizard.setData(new JSONArray(storedData));
        } catch (JSONException e) {
            log.error("Unhandled exception", e);
        }
    }

    @Override
    protected void onStart() {
        MainApp.bus().register(this);
        super.onStart();
        disposable.add(RxBus.INSTANCE
                .toObservable(EventNewNotification.class)
                .observeOn(Schedulers.io())
                .subscribe(
                        eventNewNotification -> {
                            if (notificationStore.add(eventNewNotification.notification))
                                MainApp.bus().post(new EventRefreshOverview("EventNewNotification"));
                        }, FabricPrivacy::logException
                ));
        disposable.add(RxBus.INSTANCE
                .toObservable(EventDismissNotification.class)
                .observeOn(Schedulers.io())
                .subscribe(
                        eventDismissNotification -> {
                            if (notificationStore.remove(eventDismissNotification.id))
                                MainApp.bus().post(new EventRefreshOverview("EventDismissNotification"));
                        }, FabricPrivacy::logException
                ));
    }

    @Override
    protected void onStop() {
        disposable.clear();
        MainApp.bus().unregister(this);
        super.onStop();
    }

    @Subscribe
    public void onStatusEvent(final EventNewNotification n) {
        if (notificationStore.add(n.notification))
            MainApp.bus().post(new EventRefreshOverview("EventNewNotification"));
    }

    @Subscribe
    public void onStatusEvent(final EventDismissNotification n) {
        if (notificationStore.remove(n.id))
            MainApp.bus().post(new EventRefreshOverview("EventDismissNotification"));
    }

    public double determineHighLine(String units) {
        double highLineSetting = SP.getDouble("high_mark", Profile.fromMgdlToUnits(OverviewPlugin.bgTargetHigh, units));
        if (highLineSetting < 1)
            highLineSetting = Profile.fromMgdlToUnits(180d, units);
        return highLineSetting;
    }

    public double determineLowLine() {
        Profile profile = ProfileFunctions.getInstance().getProfile();
        if (profile == null) {
            return bgTargetLow;
        }
        return determineLowLine(profile.getUnits());
    }

    public double determineLowLine(String units) {
        double lowLineSetting = SP.getDouble("low_mark", Profile.fromMgdlToUnits(OverviewPlugin.bgTargetLow, units));
        if (lowLineSetting < 1)
            lowLineSetting = Profile.fromMgdlToUnits(76d, units);
        return lowLineSetting;
    }

}
