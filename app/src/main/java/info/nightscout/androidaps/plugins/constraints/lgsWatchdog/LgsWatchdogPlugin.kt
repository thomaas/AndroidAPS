package info.nightscout.androidaps.plugins.constraints.lgsWatchdog

import androidx.annotation.NonNull
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.data.Profile
import info.nightscout.androidaps.interfaces.*
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.configBuilder.ProfileFunctions
import info.nightscout.androidaps.plugins.general.overview.events.EventDismissNotification
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.IobCobCalculatorPlugin
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.events.EventAutosensCalculationFinished
import info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.plusAssign
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object LgsWatchdogPlugin : PluginBase(PluginDescription()
        .mainType(PluginType.CONSTRAINTS)
        .neverVisible(true)
        .alwaysEnabled(true)
        .showInList(false)
        .pluginName(R.string.lqswatchdog)), ConstraintsInterface {

    private val log = LoggerFactory.getLogger(L.CONSTRAINTS)
    private var disposable: CompositeDisposable = CompositeDisposable()

    private const val MINUTES_TO_CHECK = 150L // 2.5 hours
    private const val PERCENT_THRESHOLD = 10.0 // total insulin given is less then 10% of insulin that would be given without any TBR within MINUTES_TO_CHECK
    private const val SAFETY_TBR_PCT = 10 // set TBR 10%

    private const val NO_PROFILE = -1.0

    private var insulinGiven = 0.0
    private var noTbrInsulin = 0.0

    private var safetyModeActive = false

    override fun onStart() {
        super.onStart()
        disposable += RxBus
                .toObservable(EventAutosensCalculationFinished::class.java)
                .observeOn(Schedulers.io())
                .subscribe({ check() }, {
                    FabricPrivacy.logException(it)
                })
    }

    override fun onStop() {
        disposable.clear()
        super.onStop()
    }

    @Synchronized
    fun check() {
        calculateInsulinGivenInThePast()
        if (L.isEnabled(L.CONSTRAINTS))
            log.debug("Interval: $MINUTES_TO_CHECK minutes. Base insulin: $noTbrInsulin, Given insulin: $insulinGiven")
        // No profile = nothing to check
        if (insulinGiven == NO_PROFILE) {
            safetyModeActive = false
            return
        }

        if (insulinGiven < noTbrInsulin * PERCENT_THRESHOLD / 100.0) {
            if (L.isEnabled(L.CONSTRAINTS))
                log.debug("Safety mode active")
            safetyModeActive = true
            val notification = Notification(Notification.LGSWATCHDOGACTIVE, MainApp.gs(R.string.lgswarning), Notification.URGENT, 30).sound(R.raw.urgentalarm)
            RxBus.send(EventNewNotification(notification))
        } else {
            if (L.isEnabled(L.CONSTRAINTS))
                log.debug("Safety mode inactive")
            safetyModeActive = false
            RxBus.send(EventDismissNotification(Notification.LGSWATCHDOGACTIVE))
        }
    }

    @NonNull
    override fun applyBasalConstraints(absoluteRate: Constraint<Double>, profile: Profile): Constraint<Double> {
        check()
        // Set safety TBR if lower (eg zero) TBR is requested
        val tbrToRun = profile.getBasal(DateUtil.now()) * SAFETY_TBR_PCT / 100
        absoluteRate.setIfGreater(tbrToRun, MainApp.gs(R.string.lgswatchdogactive), this)
        return absoluteRate
    }

    @NonNull
    override fun applyBasalPercentConstraints(percentRate: Constraint<Int>, profile: Profile): Constraint<Int> {
        check()
        // Set safety TBR if lower (eg zero) TBR is requested
        percentRate.setIfGreater(SAFETY_TBR_PCT, MainApp.gs(R.string.lgswatchdogactive), this)
        return percentRate
    }

    private fun calculateInsulinGivenInThePast() {
        insulinGiven = 0.0
        noTbrInsulin = 0.0
        val now = DateUtil.now()
        val startTime = IobCobCalculatorPlugin.roundUpTime(now - TimeUnit.MINUTES.toMillis(MINUTES_TO_CHECK))
        for (time in startTime..now step TimeUnit.MINUTES.toMillis(5)) {
            val profile = ProfileFunctions.getInstance().profile
            if (profile == null) {
                insulinGiven = NO_PROFILE
                return
            }
            val commonRate = profile.getBasal(time)
            val tbr = TreatmentsPlugin.getPlugin().getTempBasalFromHistory(time)
            insulinGiven = (tbr?.tempBasalConvertedToAbsolute(time, profile)
                    ?: commonRate) * TimeUnit.MINUTES.toMillis(5) / TimeUnit.MINUTES.toMillis(60)
            noTbrInsulin += commonRate * TimeUnit.MINUTES.toMillis(5) / TimeUnit.MINUTES.toMillis(60)
            // treatments
            val treatments = TreatmentsPlugin.getPlugin().getTreatments5MinBackFromHistory(time)
            for (treatment in treatments) {
                insulinGiven += treatment.insulin
            }
        }
    }

}
