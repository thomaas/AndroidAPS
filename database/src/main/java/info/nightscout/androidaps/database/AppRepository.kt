package info.nightscout.androidaps.database

import android.content.Context
import androidx.room.Room
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink
import info.nightscout.androidaps.database.transactions.InsightHistoryTransaction
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

object AppRepository {

    private const val DB_FILE = "AndroidAPS.db"

    private lateinit var database: AppDatabase;

    fun initialize(context: Context) {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE).build()
    }

    fun createOrUpdateBasedOnTimestamp(glucoseValue: GlucoseValue): Single<Boolean> = Single.fromCallable {
        database.glucoseValueDao.createOrUpdateBasedOnTimestamp(glucoseValue)
    }.subscribeOn(Schedulers.io())

    fun update(glucoseValue: GlucoseValue): Single<Long> = Single.fromCallable {
        database.glucoseValueDao.updateExistingEntry(glucoseValue)
    }

    fun getLastGlucoseValue(): Maybe<GlucoseValue> = database.glucoseValueDao.getLastGlucoseValue().subscribeOn(Schedulers.io())

    fun getLastRecentGlucoseValue(): Maybe<GlucoseValue> = System.currentTimeMillis().let {
        database.glucoseValueDao.getLastGlucoseValueInTimeRange(it - TimeUnit.MINUTES.toMillis(9), it)
    }.subscribeOn(Schedulers.io())

    fun getGlucoseValuesInTimeRange(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getProperGlucoseValuesInTimeRange(start: Long, end: Long): Single<List<GlucoseValue>> = database.glucoseValueDao.getProperGlucoseValuesInTimeRange(start, end).subscribeOn(Schedulers.io())

    fun getProperGlucoseValuesInTimeRange(timeRange: Long): Flowable<List<GlucoseValue>> = database.glucoseValueDao.getProperGlucoseValuesInTimeRange(timeRange).subscribeOn(Schedulers.io())

    fun insert(therapyEvent: TherapyEvent): Single<Long> = Single.fromCallable {
        database.therapyEventDao.insertNewEntry(therapyEvent)
    }.subscribeOn(Schedulers.io())

    /*------------------ INSIGHT ------------------*/

    fun processInsightHistoryTransaction(transaction: InsightHistoryTransaction): Completable = Completable.fromCallable {
        database.runInTransaction {
            processInsightBoluses(transaction.pumpSerial, transaction.boluses)
            processInsightTBRs(transaction.pumpSerial, transaction.temporaryBasals.toMutableList())
            processInsightTherapyEvents(transaction.pumpSerial, transaction.therapyEvents)
            processInsightTotalDailyDoses(transaction.pumpSerial, transaction.totalDailyDoses)
            processInsightOperatingModeChanges(transaction.pumpSerial, transaction.operatingModeChanges)
        }
    }

    private fun processInsightTotalDailyDoses(pumpSerial: String, tdds: List<InsightHistoryTransaction.TotalDailyDose>) {
        tdds.forEach {
            it.databaseId = database.totalDailyDoseDao.insertNewEntry(TotalDailyDose(
                    utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                    timestamp = it.timestamp,
                    bolusAmount = it.bolusAmount,
                    basalAmount = it.basalAmount,
                    totalAmount = it.bolusAmount + it.basalAmount
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = it.eventId
            })
        }
    }

    private fun processInsightTherapyEvents(pumpSerial: String, therapyEvents: List<InsightHistoryTransaction.TherapyEvent>) {
        var lastStopped: Long? = null
        therapyEvents.forEach {
            val timestamp = it.timestamp
            val eventId = it.eventId
            val type = when (it.type) {
                InsightHistoryTransaction.TherapyEvent.Type.CANNULA_FILLED -> TherapyEvent.Type.CANNULA_CHANGED
                InsightHistoryTransaction.TherapyEvent.Type.OCCLUSION -> TherapyEvent.Type.OCCLUSION
                InsightHistoryTransaction.TherapyEvent.Type.TUBE_FILLED -> TherapyEvent.Type.TUBE_CHANGED
                InsightHistoryTransaction.TherapyEvent.Type.RESERVOIR_CHANGED -> TherapyEvent.Type.RESERVOIR_CHANGED
                InsightHistoryTransaction.TherapyEvent.Type.BATTERY_EMPTY -> TherapyEvent.Type.BATTERY_EMPTY
                InsightHistoryTransaction.TherapyEvent.Type.RESERVOIR_EMPTY -> TherapyEvent.Type.RESERVOIR_EMPTY
                InsightHistoryTransaction.TherapyEvent.Type.BATTERY_CHANGED -> TherapyEvent.Type.BATTERY_CHANGED
            }
            it.databaseId = database.therapyEventDao.insertNewEntry(TherapyEvent(
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    timestamp = timestamp,
                    type = type
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = eventId
            })
        }
    }

    private fun processInsightOperatingModeChanges(pumpSerial: String, operatingModeChanges: List<InsightHistoryTransaction.OperatingModeChange>) {
        var lastStoppedEvent: InsightHistoryTransaction.OperatingModeChange? = null
        operatingModeChanges.forEach {
            val therapyEventType = when (it.to) {
                InsightHistoryTransaction.OperatingModeChange.OperatingMode.STARTED -> {
                    if (it.from == InsightHistoryTransaction.OperatingModeChange.OperatingMode.STOPPED) {
                        val lastStopped = if (lastStoppedEvent == null) {
                            var stopEvent = database.therapyEventDao.getOperatingModeEventForPumpWithSmallerPumpId(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, it.eventId)
                            if (stopEvent != null) {
                                if (stopEvent.type == TherapyEvent.Type.PUMP_PAUSED) {
                                    val possiblePreviousStopEvent = database.therapyEventDao.getOperatingModeEventForPumpWithSmallerPumpId(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, it.timestamp)
                                    if (possiblePreviousStopEvent != null && possiblePreviousStopEvent.type == TherapyEvent.Type.PUMP_STOPPED) stopEvent = possiblePreviousStopEvent
                                }
                                if (stopEvent.type == TherapyEvent.Type.PUMP_STOPPED || stopEvent.type == TherapyEvent.Type.PUMP_PAUSED) {
                                    stopEvent.timestamp
                                }
                                null
                            } else {
                                null
                            }
                        } else {
                            lastStoppedEvent!!.timestamp
                        }
                        if (lastStopped != null) {
                            it.tbrDatabaseId = database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                                    utcOffset = TimeZone.getDefault().getOffset(lastStopped).toLong(),
                                    timestamp = lastStopped,
                                    duration = it.timestamp - lastStopped,
                                    absolute = false,
                                    rate = 0.0,
                                    type = TemporaryBasal.Type.PUMP_SUSPEND
                            ).apply {
                                interfaceIDs.pumpSerial = pumpSerial
                                interfaceIDs.pumpId = it.eventId
                            })
                        }
                    }
                    TherapyEvent.Type.PUMP_STARTED
                }
                InsightHistoryTransaction.OperatingModeChange.OperatingMode.PAUSED -> {
                    TherapyEvent.Type.PUMP_PAUSED
                }
                InsightHistoryTransaction.OperatingModeChange.OperatingMode.STOPPED -> {
                    lastStoppedEvent = it
                    TherapyEvent.Type.PUMP_STOPPED
                }
            }
            it.therapyEventDatabaseId = database.therapyEventDao.insertNewEntry(TherapyEvent(
                    utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                    timestamp = it.timestamp,
                    type = therapyEventType
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = it.eventId
            })
        }
    }

    private fun processInsightTBRs(pumpSerial: String, tbrs: MutableList<InsightHistoryTransaction.TemporaryBasal>) {
        tbrs.firstOrNull()?.let {
            if (!it.start) {
                var temporaryBasal = database.temporaryBasalDao.getWithSmallerStartId_Within24Hours_WithPumpSerial_PumpAndEndIdAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, it.timestamp, it.eventId)
                if (temporaryBasal != null) {
                    temporaryBasal.duration = it.duration
                    temporaryBasal.interfaceIDs.endId = it.eventId
                    database.temporaryBasalDao.updateExistingEntry(temporaryBasal)
                } else {
                    temporaryBasal = TemporaryBasal(
                            utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                            timestamp = it.timestamp,
                            absolute = false,
                            rate = it.percentage.toDouble(),
                            duration = it.duration,
                            type = TemporaryBasal.Type.NORMAL
                    )
                    temporaryBasal.interfaceIDs.pumpSerial = pumpSerial
                    temporaryBasal.interfaceIDs.endId = it.eventId
                    database.temporaryBasalDao.insertNewEntry(temporaryBasal)
                }
                it.databaseId = temporaryBasal.id
                tbrs.removeAt(0)
            }
        }
        val tbrPairs = mutableListOf<Pair<InsightHistoryTransaction.TemporaryBasal, InsightHistoryTransaction.TemporaryBasal?>>()
        for (i in 0 until tbrs.size step 2) {
            val first = tbrs.get(i)
            val second = tbrs.getOrNull(i + 1)
            if (!first.start || second?.start == true) throw IllegalArgumentException("InsightHistoryTransaction.TemporaryBasals must be alternately stand and end events.")
            tbrPairs.add(tbrs.get(i) to tbrs.getOrNull(i + 1))
        }
        tbrPairs.forEach {
            val percentage = it.first.percentage
            val duration = it.second?.duration ?: it.first.duration
            val timestamp = it.first.timestamp
            val id = database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    timestamp = timestamp,
                    absolute = false,
                    rate = percentage.toDouble(),
                    duration = duration,
                    type = TemporaryBasal.Type.NORMAL
            ))
            it.first.databaseId = id
            it.second?.databaseId = id
        }
    }

    private fun processInsightBoluses(pumpSerial: String, boluses: List<InsightHistoryTransaction.Bolus>) {
        val groupedBoluses = boluses.groupBy { it.bolusId }
        val bolusPairs = groupedBoluses.map { entry -> entry.value.find { it.start } to entry.value.findLast { !it.start } }
        bolusPairs.forEach {
            val type = it.first?.type ?: it.second!!.type
            val bolusId = (it.first?.bolusId ?: it.second!!.bolusId).toLong()
            val timestamp = it.first?.timestamp ?: it.second!!.timestamp
            val immediateAmount = it.second?.immediateAmount ?: it.first!!.immediateAmount
            val extendedAmount = it.second?.extendedAmount ?: it.first!!.extendedAmount
            val duration = it.second?.duration ?: it.first!!.duration
            val startId = it.first?.eventId
            val endId = it.second?.eventId
            var bolusDatabaseId: Long? = null
            var extendedBolusDatabaseId: Long? = null

            if (type == InsightHistoryTransaction.Bolus.Type.STANDARD || type == InsightHistoryTransaction.Bolus.Type.MULTIWAVE) {
                var bolus = when {
                    startId == null && endId != null -> {
                        database.bolusDao.findByPumpId_StartIdIsNotNull_EndIdIsNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
                    }
                    else -> {
                        database.bolusDao.findByPumpId_StartAndEndIDsAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
                    }
                }
                if (bolus != null) {
                    bolus.amount = immediateAmount
                    if (startId != null) bolus.interfaceIDs.startId = startId
                    if (endId != null) bolus.interfaceIDs.endId = endId
                    database.bolusDao.updateExistingEntry(bolus)
                } else {
                    bolus = Bolus(
                            utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                            timestamp = timestamp,
                            amount = immediateAmount,
                            type = Bolus.Type.NORMAL,
                            basalInsulin = false
                    )
                    bolus.interfaceIDs.pumpId = bolusId
                    if (startId != null) bolus.interfaceIDs.startId = startId
                    if (endId != null) bolus.interfaceIDs.endId = endId
                    bolus.interfaceIDs.pumpSerial = pumpSerial
                    database.bolusDao.insertNewEntry(bolus)
                }
                bolusDatabaseId = bolus.id
                it.first?.bolusDatabaseId = bolus.id
                it.second?.bolusDatabaseId = bolus.id
            }

            if (type == InsightHistoryTransaction.Bolus.Type.EXTENDED || type == InsightHistoryTransaction.Bolus.Type.MULTIWAVE) {
                var extendedBolus = when {
                    startId == null && endId != null -> {
                        database.extendedBolusDao.findByPumpId_StartIdIsNotNull_EndIdIsNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
                    }
                    else -> {
                        database.extendedBolusDao.findByPumpId_StartAndEndIDsAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
                    }
                }
                if (extendedBolus != null) {
                    extendedBolus.amount = extendedAmount
                    extendedBolus.duration = duration
                    if (startId != null) extendedBolus.interfaceIDs.startId = startId
                    if (endId != null) extendedBolus.interfaceIDs.endId = endId
                    database.extendedBolusDao.updateExistingEntry(extendedBolus)
                } else {
                    extendedBolus = ExtendedBolus(
                            utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                            timestamp = timestamp,
                            amount = extendedAmount,
                            duration = duration,
                            emulatingTempBasal = false
                    )
                    if (startId != null) extendedBolus.interfaceIDs.startId = startId
                    if (endId != null) extendedBolus.interfaceIDs.endId = endId
                    extendedBolus.interfaceIDs.pumpSerial = pumpSerial
                    database.extendedBolusDao.insertNewEntry(extendedBolus)
                }
                extendedBolusDatabaseId = extendedBolus.id
                it.first?.extendedBolusDatabaseId = extendedBolus.id
                it.second?.extendedBolusDatabaseId = extendedBolus.id
            }

            if (type == InsightHistoryTransaction.Bolus.Type.MULTIWAVE) {
                var multiwaveBolusLink = when {
                    startId == null && endId != null -> {
                        database.multiwaveBolusLinkDao.findByPumpId_StartIdIsNotNull_EndIdIsNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
                    }
                    else -> {
                        database.multiwaveBolusLinkDao.findByPumpId_StartAndEndIDsAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
                    }
                }
                if (multiwaveBolusLink != null) {
                    if (startId != null) multiwaveBolusLink.interfaceIDs.startId = startId
                    if (endId != null) multiwaveBolusLink.interfaceIDs.endId = endId
                    database.multiwaveBolusLinkDao.updateExistingEntry(multiwaveBolusLink)
                } else {
                    multiwaveBolusLink = MultiwaveBolusLink(
                            bolusID = bolusDatabaseId!!,
                            extendedBolusID = extendedBolusDatabaseId!!
                    )
                    if (startId != null) multiwaveBolusLink.interfaceIDs.startId = startId
                    if (endId != null) multiwaveBolusLink.interfaceIDs.endId = endId
                    multiwaveBolusLink.interfaceIDs.pumpSerial = pumpSerial
                    database.multiwaveBolusLinkDao.insertNewEntry(multiwaveBolusLink)
                }
                it.first?.multiwaveBolusLinkDatabaseId = multiwaveBolusLink.id
                it.second?.multiwaveBolusLinkDatabaseId = multiwaveBolusLink.id
            }
        }
    }
}