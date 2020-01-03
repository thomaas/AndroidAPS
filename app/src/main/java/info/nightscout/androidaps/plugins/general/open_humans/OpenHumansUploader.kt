package info.nightscout.androidaps.plugins.general.open_humans

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import info.nightscout.androidaps.BuildConfig
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.entities.links.APSResultLink
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.interfaces.PluginDescription
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.plugins.general.open_humans.activities.OHWelcomeActivity
import info.nightscout.androidaps.plugins.general.open_humans.properties.OAuthTokenProperty
import info.nightscout.androidaps.plugins.general.open_humans.properties.ProjectMemberIdProperty
import info.nightscout.androidaps.plugins.general.open_humans.properties.UploadCounterProperty
import info.nightscout.androidaps.utils.SP
import io.reactivex.Completable
import io.reactivex.Single
import java.util.concurrent.TimeUnit

object OpenHumansUploader : PluginBase(PluginDescription()
        .pluginName(R.string.open_humans)
        .description(R.string.donate_your_data_for_science)
        .shortName(R.string.open_humans_short)
        .preferencesId(R.xml.pref_open_humans)
        .mainType(PluginType.GENERAL)), SharedPreferences.OnSharedPreferenceChangeListener {

    const val PREFERENCES_FILE = "OpenHumans"
    const val OPEN_HUMANS_URL = "https://www.openhumans.org"
    const val CLIENT_ID = "oie6DvnaEOagTxSoD6BukkLPwDhVr6cMlN74Ihz1"
    const val CLIENT_SECRET = "jR0N8pkH1jOwtozHc7CsB1UPcJzFN95ldHcK4VGYIApecr8zGJox0v06xLwPLMASScngT12aIaIHXAVCJeKquEXAWG1XekZdbubSpccgNiQBmuVmIF8nc1xSKSNJltCf"
    const val REDIRECT_URL = "androidaps://setup-openhumans"
    const val AUTH_URL = "https://www.openhumans.org/direct-sharing/projects/oauth2/authorize/?client_id=$CLIENT_ID&response_type=code"
    const val FILE_FORMAT_VERSION = 1
    const val WORK_NAME = "Open Humans"

    private val sharedPreferences = MainApp.instance().getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
    private val openHumansAPI = OpenHumansAPI(OPEN_HUMANS_URL, CLIENT_ID, CLIENT_SECRET, REDIRECT_URL)

    var projectMemberId by ProjectMemberIdProperty(sharedPreferences)
    var oAuthTokens by OAuthTokenProperty(sharedPreferences)
    var uploadCounter by UploadCounterProperty(sharedPreferences, this::projectMemberId)
    var loginStateListeners = mutableListOf<() -> Unit>()

    private fun getUploadOffsetForTable(tableName: String) = sharedPreferences.getLong("Offset_${projectMemberId!!}_$tableName", 0)

    private fun SharedPreferences.Editor.setUploadOffsetToHighest(tableName: String, entries: List<DBEntry>): SharedPreferences.Editor {
        entries.maxBy { it.id }?.id?.let { putLong("Offset_${projectMemberId!!}_$tableName", it + 1) }
        return this
    }

    private fun scheduleWorker() {
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(SP.getBoolean(R.string.key_oh_charging_only, false))
                .build()
        val workRequest = PeriodicWorkRequestBuilder<OHUploadWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS)
                .build()
        WorkManager.getInstance(MainApp.instance()).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, workRequest)
    }

    private fun cancelWorker() {
        WorkManager.getInstance(MainApp.instance()).cancelUniqueWork(WORK_NAME)
    }

    override fun onStart() {
        if (projectMemberId != null) scheduleWorker()
        setupNotificationChannel()
        SP.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        cancelWorker()
        SP.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    fun registerLoginStateListener(listener: () -> Unit) {
        synchronized(loginStateListeners) {
            loginStateListeners.add(listener)
        }
    }

    fun unregisterLoginStateListener(listener: () -> Unit) {
        synchronized(loginStateListeners) {
            loginStateListeners.remove(listener)
        }
    }

    fun logout() {
        cancelWorker()
        oAuthTokens = null
        projectMemberId = null
        synchronized(loginStateListeners) {
            loginStateListeners.forEach {
                try {
                    it()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun login(authCode: String): Completable =
            openHumansAPI.exchangeAuthToken(authCode)
                    .flatMap {
                        oAuthTokens = it
                        openHumansAPI.getProjectMemberId(it.accessToken)
                    }.doOnSuccess {
                        projectMemberId = it
                        scheduleWorker()
                        synchronized(loginStateListeners) {
                            loginStateListeners.forEach {
                                try {
                                    it()
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }.doOnError {
                        oAuthTokens = null
                        projectMemberId = null
                    }
                    .ignoreElement()

    private fun uploadFile(uploadData: UploadData) =
            refreshAccessTokenIfNeeded()
                    .andThen(openHumansAPI.prepareFileUpload(oAuthTokens!!.accessToken, uploadData.fileName, OpenHumansAPI.FileMetadata(
                            tags = uploadData.tags,
                            description = "AndroidAPS Database Upload",
                            md5 = uploadData.zipMd5,
                            creationDate = uploadData.timestamp,
                            startDate = uploadData.lowestTimestamp,
                            endDate = uploadData.highestTimestamp
                    )))
                    .flatMap { openHumansAPI.uploadFile(it.uploadURL, uploadData.zip).andThen(Single.just(it.fileId)) }
                    .flatMapCompletable { openHumansAPI.completeFileUpload(oAuthTokens!!.accessToken, it) }

    private fun refreshAccessTokenIfNeeded() = Completable.defer {
        if (oAuthTokens!!.expiresAt <= System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)) openHumansAPI.refreshToken(oAuthTokens!!.refreshToken).doOnSuccess { oAuthTokens = it }.ignoreElement()
        else Completable.complete()
    }

    private fun gatherData() = Single.defer {
        Single.zipArray({
            val test = getUploadOffsetForTable("GlucoseValues")
            val hasGitInfo = !BuildConfig.HEAD.endsWith("NoGitSystemAvailable", true)
            val customRemote = !BuildConfig.REMOTE.equals("https://github.com/MilosKozak/AndroidAPS.git", true)
            @Suppress("UNCHECKED_CAST")
            (UploadData(
                    timestamp = System.currentTimeMillis(),
                    uploadCounter = uploadCounter,
                    fileFormatVersion = FILE_FORMAT_VERSION,
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    databaseVersion = AppRepository.databaseVersion,
                    hasGitInfo = hasGitInfo,
                    commitHash = if (hasGitInfo && !customRemote) BuildConfig.HEAD else null,
                    customRemote = customRemote && hasGitInfo,
                    applicationId = MainApp.getApplicationId(),
                    apsResultLinks = it[0] as List<APSResultLink>,
                    mealLinks = it[1] as List<MealLink>,
                    multiwaveBolusLinks = it[2] as List<MultiwaveBolusLink>,
                    apsResults = it[3] as List<APSResult>,
                    boluses = it[4] as List<Bolus>,
                    bolusCalculatorResults = it[5] as List<BolusCalculatorResult>,
                    carbs = it[6] as List<Carbs>,
                    effectiveProfileSwitches = it[7] as List<EffectiveProfileSwitch>,
                    extendedBoluses = it[8] as List<ExtendedBolus>,
                    glucoseValues = it[9] as List<GlucoseValue>,
                    profileSwitches = it[10] as List<ProfileSwitch>,
                    temporaryBasals = it[11] as List<TemporaryBasal>,
                    temporaryTargets = it[12] as List<TemporaryTarget>,
                    therapyEvents = it[13] as List<TherapyEvent>,
                    totalDailyDoses = it[14] as List<TotalDailyDose>,
                    versionChanges = it[15] as List<VersionChange>,
                    preferenceChanges = it[16] as List<PreferenceChange>
            ))
        }, arrayOf(
                AppRepository.getAllChangedAPSResultLinksStartingFrom(getUploadOffsetForTable("APSResultLinks")),
                AppRepository.getAllChangedMealLinksStartingFrom(getUploadOffsetForTable("MealLinks")),
                AppRepository.getAllChangedMultiwaveBolusLinksStartingFrom(getUploadOffsetForTable("MultiwaveBolusLinks")),
                AppRepository.getAllChangedAPSResultsStartingFrom(getUploadOffsetForTable("APSResults")),
                AppRepository.getAllChangedBolusesStartingFrom(getUploadOffsetForTable("Boluses")),
                AppRepository.getAllChangedBolusCalculatorResultsStartingFrom(getUploadOffsetForTable("BolusCalculatorResults")),
                AppRepository.getAllChangedCarbsStartingFrom(getUploadOffsetForTable("Carbs")),
                AppRepository.getAllChangedEffectiveProfileSwitchesStartingFrom(getUploadOffsetForTable("EffectiveProfileSwitches")),
                AppRepository.getAllChangedExtendedBolusesStartingFrom(getUploadOffsetForTable("ExtendedBoluses")),
                AppRepository.getAllChangedGlucoseValuesStartingFrom(getUploadOffsetForTable("GlucoseValues")),
                AppRepository.getAllChangedProfileSwitchesStartingFrom(getUploadOffsetForTable("ProfileSwitches")),
                AppRepository.getAllChangedTemporaryBasalsStartingFrom(getUploadOffsetForTable("TemporaryBasals")),
                AppRepository.getAllChangedTemporaryTargetsStartingFrom(getUploadOffsetForTable("TemporaryTargets")),
                AppRepository.getAllChangedTherapyEventsStartingFrom(getUploadOffsetForTable("TherapyEvents")),
                AppRepository.getAllChangedTotalDailyDosesStartingFrom(getUploadOffsetForTable("TotalDailyDoses")),
                AppRepository.getAllVersionChangesStartingFrom(getUploadOffsetForTable("VersionChanges")),
                AppRepository.getAllPreferenceChangesStartingFrom(getUploadOffsetForTable("PreferenceChanges"))
        ))
    }

    @SuppressLint("ApplySharedPref")
    private fun adjustCounters(uploadData: UploadData) = Completable.fromCallable {
        sharedPreferences.edit()
                .setUploadOffsetToHighest("APSResultLinks", uploadData.apsResultLinks)
                .setUploadOffsetToHighest("MealLinks", uploadData.mealLinks)
                .setUploadOffsetToHighest("MultiwaveBolusLinks", uploadData.multiwaveBolusLinks)
                .setUploadOffsetToHighest("APSResults", uploadData.apsResults)
                .setUploadOffsetToHighest("Boluses", uploadData.boluses)
                .setUploadOffsetToHighest("BolusCalculatorResults", uploadData.bolusCalculatorResults)
                .setUploadOffsetToHighest("Carbs", uploadData.carbs)
                .setUploadOffsetToHighest("EffectiveProfileSwitches", uploadData.effectiveProfileSwitches)
                .setUploadOffsetToHighest("ExtendedBoluses", uploadData.extendedBoluses)
                .setUploadOffsetToHighest("GlucoseValues", uploadData.glucoseValues)
                .setUploadOffsetToHighest("ProfileSwitches", uploadData.profileSwitches)
                .setUploadOffsetToHighest("TemporaryBasals", uploadData.temporaryBasals)
                .setUploadOffsetToHighest("TemporaryTargets", uploadData.temporaryTargets)
                .setUploadOffsetToHighest("TherapyEvents", uploadData.therapyEvents)
                .setUploadOffsetToHighest("TotalDailyDoses", uploadData.totalDailyDoses)
                .setUploadOffsetToHighest("VersionChanges", uploadData.versionChanges)
                .setUploadOffsetToHighest("PreferenceChanges", uploadData.preferenceChanges)
                .commit()
        uploadCounter = uploadData.uploadCounter + 1
        null
    }

    fun uploadData() = gatherData()
            .flatMap { uploadFile(it).andThen(Single.just(it)) }
            .flatMapCompletable(this::adjustCounters)
            .doOnError {
                if (it is OpenHumansAPI.OHErrorneousResultException && it.code == 401) {
                    when (it.detail) {
                        "Expired token." -> oAuthTokens = oAuthTokens!!.copy(expiresAt = -1)
                        "Invalid token." -> handleSignOut()
                    }

                }
            }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManagerCompat = NotificationManagerCompat.from(MainApp.instance())
            notificationManagerCompat.createNotificationChannel(NotificationChannel(
                    "OpenHumans",
                    MainApp.gs(R.string.open_humans),
                    NotificationManager.IMPORTANCE_DEFAULT
            ))
        }
    }

    private fun handleSignOut() {
        oAuthTokens = null
        projectMemberId = null
        synchronized(loginStateListeners) {
            loginStateListeners.forEach {
                try {
                    it()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        cancelWorker()
        val notification = NotificationCompat.Builder(MainApp.instance(), "OpenHumans")
                .setContentTitle(MainApp.gs(R.string.you_have_been_signed_out_of_open_humans))
                .setContentText(MainApp.gs(R.string.click_here_to_sign_in_again_if_this_was_a_mistake))
                .setStyle(NotificationCompat.BigTextStyle())
                .setSmallIcon(R.drawable.notif_icon)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(
                        MainApp.instance(),
                        0,
                        Intent(MainApp.instance(), OHWelcomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        0
                ))
                .build()
        NotificationManagerCompat.from(MainApp.instance()).notify(3123, notification)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == MainApp.gs(R.string.key_oh_charging_only) && projectMemberId != null) scheduleWorker()
    }
}