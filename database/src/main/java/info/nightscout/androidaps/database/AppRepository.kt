package info.nightscout.androidaps.database

import android.content.Context
import androidx.room.Room
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.entities.links.APSResultLink
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink
import info.nightscout.androidaps.database.interfaces.DBEntry
import info.nightscout.androidaps.database.transactions.MergedBolus
import info.nightscout.androidaps.database.transactions.Transaction
import io.reactivex.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

object AppRepository {

    private const val DB_FILE = "AndroidAPS.db"

    internal lateinit var database: AppDatabase;

    private val changeSubject = PublishSubject.create<List<DBEntry>>()

    val changeObservable: Observable<List<DBEntry>> = changeSubject

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

    fun getTotalDailyDoses(amount: Int): Single<List<TotalDailyDose>> = database.totalDailyDoseDao.getTotalDailyDoses(amount)

    fun getLastTherapyEventByType(type: TherapyEvent.Type): Maybe<TherapyEvent> = database.therapyEventDao.getLastTherapyEventByType(type).subscribeOn(Schedulers.io())

    fun getTherapyEventsInTimeRange(start: Long, end: Long): Flowable<List<TherapyEvent>> = database.therapyEventDao.getTherapyEventsInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getTherapyEventsInTimeRange(type: TherapyEvent.Type, start: Long, end: Long): Flowable<List<TherapyEvent>> = database.therapyEventDao.getTherapyEventsInTimeRange(type, start, end).subscribeOn(Schedulers.io())

    fun getAllTherapyEvents(): Flowable<List<TherapyEvent>> = database.therapyEventDao.getAllTherapyEvents().subscribeOn(Schedulers.io())

    fun getProfileSwitchesInTimeRange(start: Long, end: Long): Flowable<List<ProfileSwitch>> = database.profileSwitchDao.getProfileSwitchesInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getAllProfileSwitches(): Flowable<List<ProfileSwitch>> = database.profileSwitchDao.getAllProfileSwitches().subscribeOn(Schedulers.io())

    fun getTemporaryBasalActiveAtIncludingInvalidMaybe(timestamp: Long, pumpType: InterfaceIDs.PumpType): Maybe<TemporaryBasal> = database.temporaryBasalDao.getTemporaryBasalActiveAtIncludingInvalidMaybe(timestamp, pumpType)

    fun getAPSResultsCreatedBetween(start: Long, end: Long): Single<List<APSResult>> = database.apsResultDao.getAllEntriesCreatedBetween(start, end)

    fun getAPSResultLinksCreatedBetween(start: Long, end: Long): Single<List<APSResultLink>> = database.apsResultLinkDao.getAllEntriesCreatedBetween(start, end)

    fun getBolusCalculatorResultsCreatedBetween(start: Long, end: Long): Single<List<BolusCalculatorResult>> = database.bolusCalculatorResultDao.getAllEntriesCreatedBetween(start, end)

    fun getBolusesCreatedBetween(start: Long, end: Long): Single<List<Bolus>> = database.bolusDao.getAllEntriesCreatedBetween(start, end)

    fun getCarbsCreatedBetween(start: Long, end: Long): Single<List<Carbs>> = database.carbsDao.getAllEntriesCreatedBetween(start, end)

    fun getEffectiveProfileSwitchesCreatedBetween(start: Long, end: Long): Single<List<EffectiveProfileSwitch>> = database.effectiveProfileSwitchDao.getAllEntriesCreatedBetween(start, end)

    fun getExtendedBolusesCreatedBetween(start: Long, end: Long): Single<List<ExtendedBolus>> = database.extendedBolusDao.getAllEntriesCreatedBetween(start, end)

    fun getGlucoseValuesCreatedBetween(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getAllEntriesCreatedBetween(start, end)

    fun getMealLinksCreatedBetween(start: Long, end: Long): Single<List<MealLink>> = database.mealLinkDao.getAllEntriesCreatedBetween(start, end)

    fun getMultiwaveBolusLinksCreatedBetween(start: Long, end: Long): Single<List<MultiwaveBolusLink>> = database.multiwaveBolusLinkDao.getAllEntriesCreatedBetween(start, end)

    fun getProfileSwitchesCreatedBetween(start: Long, end: Long): Single<List<ProfileSwitch>> = database.profileSwitchDao.getAllEntriesCreatedBetween(start, end)

    fun getTemporaryBasalsCreatedBetween(start: Long, end: Long): Single<List<TemporaryBasal>> = database.temporaryBasalDao.getAllEntriesCreatedBetween(start, end)

    fun getTemporaryTargetsCreatedBetween(start: Long, end: Long): Single<List<TemporaryTarget>> = database.temporaryTargetDao.getAllEntriesCreatedBetween(start, end)

    fun getTherapyEventsCreatedBetween(start: Long, end: Long): Single<List<TherapyEvent>> = database.therapyEventDao.getAllEntriesCreatedBetween(start, end)

    fun getTotalDailyDosesCreatedBetween(start: Long, end: Long): Single<List<TotalDailyDose>> = database.totalDailyDoseDao.getAllEntriesCreatedBetween(start, end)

    fun getMergedBolusData(start: Long, end: Long) = Single.fromCallable {
        val boluses = database.bolusDao.getBolusesInTimeRange(start, end)
        val carbs = database.carbsDao.getCarbsInTimeRange(start, end).toMutableList()
        val mergedBoluses = mutableListOf<MergedBolus>()
        boluses.forEach {
            val mealLink = database.mealLinkDao.findByBolusId(it.id)
            if (mealLink != null) {
                var carbEntry = mealLink.carbsId?.run { carbs.find { it.id == this } ?: AppRepository.database.carbsDao.findById(this) }
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