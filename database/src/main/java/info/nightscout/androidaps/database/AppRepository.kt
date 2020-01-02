package info.nightscout.androidaps.database

import android.content.Context
import androidx.room.Room
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.entities.links.APSResultLink
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.interfaces.TraceableDBEntry
import info.nightscout.androidaps.database.transactions.Transaction
import info.nightscout.androidaps.database.transactions.treatments.MergedBolus
import io.reactivex.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

object AppRepository {

    private const val DB_FILE = "AndroidAPS.db"

    internal lateinit var database: AppDatabase

    private val changeSubject = PublishSubject.create<List<DBEntry>>()

    val changeObservable: Observable<List<DBEntry>> = changeSubject

    val databaseVersion = DATABASE_VERSION

    fun initialize(context: Context) {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE).build()
    }

    fun <T> runTransaction(transaction: Transaction<T>): Completable {
        val changes = mutableListOf<DBEntry>()
        return Completable.fromCallable {
            database.runInTransaction {
                transaction.database = DelegatedAppDatabase(changes, database)
                transaction.run()
            }
        }.subscribeOn(Schedulers.io()).doOnComplete {
            changeSubject.onNext(changes)
        }
    }

    fun <T> runTransactionForResult(transaction: Transaction<T>): Single<T> {
        val changes = mutableListOf<DBEntry>()
        return Single.fromCallable {
            database.runInTransaction(Callable<T> {
                transaction.database = DelegatedAppDatabase(changes, database)
                transaction.run()
            })
        }.subscribeOn(Schedulers.io()).doOnSuccess {
            changeSubject.onNext(changes)
        }
    }

    fun getLastGlucoseValue(): Maybe<GlucoseValue> = database.glucoseValueDao.getLastGlucoseValue().subscribeOn(Schedulers.io())

    fun getLastGlucoseValueIfRecent(): Maybe<GlucoseValue> = System.currentTimeMillis().let {
        database.glucoseValueDao.getLastGlucoseValueInTimeRange(it - TimeUnit.MINUTES.toMillis(9), it)
    }.subscribeOn(Schedulers.io())

    fun getGlucoseValuesInTimeRange(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getGlucoseValuesInTimeRangeIf39OrHigher(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getGlucoseValuesInTimeRangeIf39OrHigher(start, end).subscribeOn(Schedulers.io())

    fun getGlucoseValuesInTimeRangeIf39OrHigher(timeRange: Long): Flowable<List<GlucoseValue>> = database.glucoseValueDao.getGlucoseValuesInTimeRangeIf39OrHigher(timeRange).subscribeOn(Schedulers.io())

    fun getTemporaryBasalsInTimeRange(start: Long, end: Long): Flowable<List<TemporaryBasal>> = database.temporaryBasalDao.getTemporaryBasalsInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getExtendedBolusesInTimeRange(start: Long, end: Long): Flowable<List<ExtendedBolus>> = database.extendedBolusDao.getExtendedBolusesInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getTemporaryTargetsInTimeRange(start: Long, end: Long): Flowable<List<TemporaryTarget>> = database.temporaryTargetDao.getTemporaryTargetsInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getTotalDailyDoses(amount: Int): Single<List<TotalDailyDose>> = database.totalDailyDoseDao.getTotalDailyDoses(amount).subscribeOn(Schedulers.io())

    fun getLastTherapyEventByType(type: TherapyEvent.Type): Maybe<TherapyEvent> = database.therapyEventDao.getLastTherapyEventByType(type).subscribeOn(Schedulers.io())

    fun getTherapyEventsInTimeRange(start: Long, end: Long): Flowable<List<TherapyEvent>> = database.therapyEventDao.getTherapyEventsInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getTherapyEventsInTimeRange(type: TherapyEvent.Type, start: Long, end: Long): Flowable<List<TherapyEvent>> = database.therapyEventDao.getTherapyEventsInTimeRange(type, start, end).subscribeOn(Schedulers.io())

    fun getAllTherapyEvents(): Flowable<List<TherapyEvent>> = database.therapyEventDao.getAllTherapyEvents().subscribeOn(Schedulers.io())

    fun getProfileSwitchesInTimeRange(start: Long, end: Long): Flowable<List<ProfileSwitch>> = database.profileSwitchDao.getProfileSwitchesInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getAllProfileSwitches(): Flowable<List<ProfileSwitch>> = database.profileSwitchDao.getAllProfileSwitches().subscribeOn(Schedulers.io())

    fun getTemporaryBasalActiveAtIncludingInvalidMaybe(timestamp: Long, pumpType: InterfaceIDs.PumpType): Maybe<TemporaryBasal> = database.temporaryBasalDao.getTemporaryBasalActiveAtIncludingInvalidMaybe(timestamp, pumpType).subscribeOn(Schedulers.io())

    private fun <T : TraceableDBEntry> List<T>.loadReferences(findById: (id: Long) -> T?): List<T> {
        val historicEntries = filter { it.historic }
        val referenceEntries = filter { !it.historic }.toMutableList()
        historicEntries.forEach { historic ->
            if (referenceEntries.firstOrNull { it.id == historic.referenceId } == null) {
                referenceEntries.add(findById(historic.referenceId!!)!!)
            }
        }
        referenceEntries.addAll(referenceEntries)
        return referenceEntries
    }

    fun getAllChangedAPSResultsStartingFrom(id: Long): Single<List<APSResult>> =
            database.apsResultDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.apsResultDao::findById) }

    fun getAllChangedAPSResultLinksStartingFrom(id: Long): Single<List<APSResultLink>> =
            database.apsResultLinkDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.apsResultLinkDao::findById) }

    fun getAllChangedBolusCalculatorResultsStartingFrom(id: Long): Single<List<BolusCalculatorResult>> =
            database.bolusCalculatorResultDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.bolusCalculatorResultDao::findById) }

    fun getAllChangedBolusesStartingFrom(id: Long): Single<List<Bolus>> =
            database.bolusDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.bolusDao::findById) }

    fun getAllChangedCarbsStartingFrom(id: Long): Single<List<Carbs>> =
            database.carbsDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.carbsDao::findById) }

    fun getAllChangedEffectiveProfileSwitchesStartingFrom(id: Long): Single<List<EffectiveProfileSwitch>> =
            database.effectiveProfileSwitchDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.effectiveProfileSwitchDao::findById) }

    fun getAllChangedExtendedBolusesStartingFrom(id: Long): Single<List<ExtendedBolus>> =
            database.extendedBolusDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.extendedBolusDao::findById) }

    fun getAllChangedGlucoseValuesStartingFrom(id: Long): Single<List<GlucoseValue>> =
            database.glucoseValueDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.glucoseValueDao::findById) }

    fun getAllChangedMealLinksStartingFrom(id: Long): Single<List<MealLink>> =
            database.mealLinkDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.mealLinkDao::findById) }

    fun getAllChangedMultiwaveBolusLinksStartingFrom(id: Long): Single<List<MultiwaveBolusLink>> =
            database.multiwaveBolusLinkDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.multiwaveBolusLinkDao::findById) }

    fun getAllPreferenceChangesStartingFrom(id: Long): Single<List<PreferenceChange>> = database.preferenceChangeDao.getAllStartingFrom(id).subscribeOn(Schedulers.io())

    fun getAllChangedProfileSwitchesStartingFrom(id: Long): Single<List<ProfileSwitch>> =
            database.profileSwitchDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.profileSwitchDao::findById) }

    fun getAllChangedTemporaryBasalsStartingFrom(id: Long): Single<List<TemporaryBasal>> =
            database.temporaryBasalDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.temporaryBasalDao::findById) }

    fun getAllChangedTemporaryTargetsStartingFrom(id: Long): Single<List<TemporaryTarget>> =
            database.temporaryTargetDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.temporaryTargetDao::findById) }

    fun getAllChangedTherapyEventsStartingFrom(id: Long): Single<List<TherapyEvent>> =
            database.therapyEventDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.therapyEventDao::findById) }

    fun getAllChangedTotalDailyDosesStartingFrom(id: Long): Single<List<TotalDailyDose>> =
            database.totalDailyDoseDao.getAllStartingFrom(id).subscribeOn(Schedulers.io()).map { it.loadReferences(database.totalDailyDoseDao::findById) }

    fun getAllVersionChangesStartingFrom(id: Long): Single<List<VersionChange>> = database.versionChangeDao.getAllStartingFrom(id).subscribeOn(Schedulers.io())

    fun getMergedBolusData(start: Long, end: Long) = Single.fromCallable {
        val boluses = database.bolusDao.getBolusesInTimeRange(start, end)
        val carbs = database.carbsDao.getCarbsInTimeRange(start, end).toMutableList()
        val mergedBoluses = mutableListOf<MergedBolus>()
        boluses.forEach {
            val mealLink = database.mealLinkDao.findByBolusId(it.id)
            if (mealLink != null) {
                var carbEntry = mealLink.carbsId?.run {
                    carbs.find { it.id == this }
                            ?: AppRepository.database.carbsDao.findById(this)
                }
                if (carbEntry != null) {
                    carbs.remove(carbEntry)
                    if (carbEntry.timestamp != it.timestamp) {
                        mergedBoluses.add(MergedBolus(
                                null,
                                null,
                                carbEntry,
                                null
                        ))
                        carbEntry = null
                    }
                }
                val bolusCalculatorResult = mealLink.bolusCalcResultId?.run { AppRepository.database.bolusCalculatorResultDao.findById(this) }
                mergedBoluses.add(MergedBolus(
                        mealLink,
                        it,
                        carbEntry,
                        bolusCalculatorResult
                ))
            } else {
                mergedBoluses.add(MergedBolus(
                        null,
                        it,
                        null,
                        null
                ))
            }
        }
        carbs.forEach {
            val mealLink = database.mealLinkDao.findByCarbsId(it.id)
            if (mealLink != null) {
                val bolus = mealLink.bolusId?.run { AppRepository.database.bolusDao.findById(this) }
                val bolusCalculatorResult = mealLink.bolusCalcResultId?.run { AppRepository.database.bolusCalculatorResultDao.findById(this) }
                mergedBoluses.add(MergedBolus(
                        mealLink,
                        bolus,
                        it,
                        bolusCalculatorResult
                ))
            } else {
                mergedBoluses.add(MergedBolus(
                        null,
                        null,
                        it,
                        null
                ))
            }
        }
        mergedBoluses
    }.subscribeOn(Schedulers.io())
}