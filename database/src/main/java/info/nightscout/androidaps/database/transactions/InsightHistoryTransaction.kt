package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.database.entities.links.MultiwaveBolusLink
import java.util.*
import java.util.concurrent.TimeUnit

class InsightHistoryTransaction(val pumpSerial: String) : Transaction<Unit>() {

    val therapyEvents = mutableListOf<TherapyEvent>()
    val temporaryBasals = mutableListOf<TemporaryBasal>()
    val boluses = mutableListOf<Bolus>()
    val totalDailyDoses = mutableListOf<TotalDailyDose>()
    val operatingModeChanges = mutableListOf<OperatingModeChange>()


    override fun process() {
        processBoluses()
        processTemporaryBasals()
        processTherapyEvents()
        processTotalDailyDoses()
        processOperatingModeChanges()
    }

    private fun processTotalDailyDoses() {
        totalDailyDoses.forEach {
            it.databaseId = AppRepository.database.totalDailyDoseDao.insertNewEntry(TotalDailyDose(
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

    private fun processTherapyEvents() {
        therapyEvents.forEach {
            val timestamp = it.timestamp
            val eventId = it.eventId
            val type = when (it.type) {
                TherapyEvent.Type.CANNULA_FILLED -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.CANNULA_CHANGED
                TherapyEvent.Type.OCCLUSION -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.OCCLUSION
                TherapyEvent.Type.TUBE_FILLED -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.TUBE_CHANGED
                TherapyEvent.Type.RESERVOIR_CHANGED -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.RESERVOIR_CHANGED
                TherapyEvent.Type.BATTERY_EMPTY -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.BATTERY_EMPTY
                TherapyEvent.Type.RESERVOIR_EMPTY -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.RESERVOIR_EMPTY
                TherapyEvent.Type.BATTERY_CHANGED -> info.nightscout.androidaps.database.entities.TherapyEvent.Type.BATTERY_CHANGED
            }
            it.databaseId = AppRepository.database.therapyEventDao.insertNewEntry(TherapyEvent(
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    timestamp = timestamp,
                    type = type
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = eventId
            })
        }
    }

    private fun getDateStopped(eventId: Long): Long? {
        val stopEvent = AppRepository.database.therapyEventDao.getOperatingModeEventForPumpWithSmallerPumpId(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, eventId)
        if (stopEvent?.type == info.nightscout.androidaps.database.entities.TherapyEvent.Type.PUMP_STOPPED) {
            val pauseEvent = AppRepository.database.therapyEventDao.getOperatingModeEventForPumpWithSmallerPumpId(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, stopEvent.interfaceIDs.pumpId!!)
            if (pauseEvent?.type == info.nightscout.androidaps.database.entities.TherapyEvent.Type.PUMP_PAUSED
                    && stopEvent.timestamp - pauseEvent.timestamp < TimeUnit.MINUTES.toMillis(20)) {
                return pauseEvent.timestamp
            }
            return stopEvent.timestamp
        }
        return null
    }

    private fun saveSuspendTBR(startEvent: OperatingModeChange, lastStoppedEvent: OperatingModeChange?, lastPausedEvent: OperatingModeChange?) {
        val dateStopped = if (lastStoppedEvent == null) {
            getDateStopped(startEvent.eventId)
        } else {
            lastPausedEvent?.timestamp ?: lastStoppedEvent.timestamp
        }
        if (dateStopped != null) {
            startEvent.tbrDatabaseId = AppRepository.database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                    utcOffset = TimeZone.getDefault().getOffset(dateStopped).toLong(),
                    timestamp = dateStopped,
                    duration = startEvent.timestamp - dateStopped,
                    absolute = false,
                    rate = 0.0,
                    type = info.nightscout.androidaps.database.entities.TemporaryBasal.Type.PUMP_SUSPEND
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = startEvent.eventId
            })
        }
    }

    private fun processOperatingModeChanges() {
        var lastStoppedEvent: OperatingModeChange? = null
        var lastPausedEvent: OperatingModeChange? = null
        operatingModeChanges.forEach {
            val therapyEventType = when (it.to) {
                OperatingModeChange.OperatingMode.STARTED -> {
                    if (it.from == OperatingModeChange.OperatingMode.STOPPED) {
                        saveSuspendTBR(it, lastStoppedEvent, lastPausedEvent)
                    }
                    info.nightscout.androidaps.database.entities.TherapyEvent.Type.PUMP_STARTED
                }
                OperatingModeChange.OperatingMode.PAUSED -> {
                    lastPausedEvent = it
                    info.nightscout.androidaps.database.entities.TherapyEvent.Type.PUMP_PAUSED
                }
                OperatingModeChange.OperatingMode.STOPPED -> {
                    lastStoppedEvent = it
                    if (it.from != OperatingModeChange.OperatingMode.PAUSED) lastPausedEvent = null
                    info.nightscout.androidaps.database.entities.TherapyEvent.Type.PUMP_STOPPED
                }
            }
            it.therapyEventDatabaseId = AppRepository.database.therapyEventDao.insertNewEntry(TherapyEvent(
                    utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                    timestamp = it.timestamp,
                    type = therapyEventType
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = it.eventId
            })
        }
    }

    private fun updateExistingTemporaryBasal(temporaryBasal: TemporaryBasal) {
        var dbTBR = AppRepository.database.temporaryBasalDao.getWithSmallerStartId_Within24Hours_WithPumpSerial_PumpAndEndIdAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, temporaryBasal.timestamp, temporaryBasal.eventId)
        if (dbTBR != null) {
            dbTBR.duration = temporaryBasal.duration
            dbTBR.interfaceIDs.endId = temporaryBasal.eventId
            AppRepository.database.temporaryBasalDao.updateExistingEntry(dbTBR)
        } else {
            dbTBR = TemporaryBasal(
                    utcOffset = TimeZone.getDefault().getOffset(temporaryBasal.timestamp).toLong(),
                    timestamp = temporaryBasal.timestamp,
                    absolute = false,
                    rate = temporaryBasal.percentage.toDouble(),
                    duration = temporaryBasal.duration,
                    type = info.nightscout.androidaps.database.entities.TemporaryBasal.Type.NORMAL
            )
            dbTBR.interfaceIDs.pumpSerial = pumpSerial
            dbTBR.interfaceIDs.endId = temporaryBasal.eventId
            AppRepository.database.temporaryBasalDao.insertNewEntry(dbTBR)
        }
        temporaryBasal.databaseId = dbTBR.id
    }

    private fun processTemporaryBasals() {
        val tbrs = temporaryBasals.toMutableList()
        tbrs.firstOrNull()?.let {
            if (!it.start) {
                updateExistingTemporaryBasal(it)
                tbrs.removeAt(0)
            }
        }
        val tbrPairs = mutableListOf<Pair<TemporaryBasal, TemporaryBasal?>>()
        for (i in 0 until tbrs.size step 2) {
            val first = tbrs.get(i)
            val second = tbrs.getOrNull(i + 1)
            if (!first.start || second?.start == true) throw IllegalArgumentException("InsightHistoryTransaction.TemporaryBasals must be alternately start and end events.")
            tbrPairs.add(tbrs.get(i) to tbrs.getOrNull(i + 1))
        }
        tbrPairs.forEach {
            val percentage = it.first.percentage
            val duration = it.second?.duration ?: it.first.duration
            val timestamp = it.first.timestamp
            val id = AppRepository.database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    timestamp = timestamp,
                    absolute = false,
                    rate = percentage.toDouble(),
                    duration = duration,
                    type = info.nightscout.androidaps.database.entities.TemporaryBasal.Type.NORMAL
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.startId = it.first.eventId
                interfaceIDs.endId = it.second?.eventId
            })
            it.first.databaseId = id
            it.second?.databaseId = id
        }
    }

    private fun saveStandardBolus(start: Bolus?, end: Bolus?): Long {
        val bolusId = (start?.bolusId ?: end!!.bolusId).toLong()
        var bolus = when {
            start == null && end != null -> {
                AppRepository.database.bolusDao.findByPumpId_StartIdIsNotNull_EndIdIsNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
            }
            else -> {
                AppRepository.database.bolusDao.findByPumpId_StartAndEndIDsAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
            }
        }
        val amount = end?.immediateAmount ?: start!!.immediateAmount
        if (bolus != null) {
            bolus.amount = amount
            if (start != null) bolus.interfaceIDs.startId = start.eventId
            if (end != null) bolus.interfaceIDs.endId = end.eventId
            AppRepository.database.bolusDao.updateExistingEntry(bolus)
        } else {
            val timestamp = start?.timestamp ?: end!!.timestamp
            bolus = Bolus(
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    timestamp = timestamp,
                    amount = amount,
                    type = info.nightscout.androidaps.database.entities.Bolus.Type.NORMAL,
                    basalInsulin = false
            )
            bolus.interfaceIDs.pumpId = bolusId
            if (start != null) bolus.interfaceIDs.startId = start.eventId
            if (end != null) bolus.interfaceIDs.endId = end.eventId
            bolus.interfaceIDs.pumpSerial = pumpSerial
            AppRepository.database.bolusDao.insertNewEntry(bolus)
        }
        start?.bolusDatabaseId = bolus.id
        end?.bolusDatabaseId = bolus.id
        return bolus.id
    }

    private fun saveExtendedBolus(start: Bolus?, end: Bolus?): Long {
        val bolusId = (start?.bolusId ?: end!!.bolusId).toLong()
        var extendedBolus = when {
            start == null && end != null -> {
                AppRepository.database.extendedBolusDao.findByPumpId_StartIdIsNotNull_EndIdIsNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
            }
            else -> {
                AppRepository.database.extendedBolusDao.findByPumpId_StartAndEndIDsAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
            }
        }
        val amount = end?.extendedAmount ?: start!!.extendedAmount
        if (extendedBolus != null) {
            extendedBolus.amount = amount
            if (start != null) extendedBolus.interfaceIDs.startId = start.eventId
            if (end != null) extendedBolus.interfaceIDs.endId = end.eventId
            AppRepository.database.extendedBolusDao.updateExistingEntry(extendedBolus)
        } else {
            val timestamp = start?.timestamp ?: end!!.timestamp
            extendedBolus = ExtendedBolus(
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    timestamp = timestamp,
                    amount = amount,
                    duration = end?.duration ?: start!!.duration,
                    emulatingTempBasal = false
            )
            extendedBolus.interfaceIDs.pumpId = bolusId
            if (start != null) extendedBolus.interfaceIDs.startId = start.eventId
            if (end != null) extendedBolus.interfaceIDs.endId = end.eventId
            extendedBolus.interfaceIDs.pumpSerial = pumpSerial
            AppRepository.database.extendedBolusDao.insertNewEntry(extendedBolus)
        }
        start?.bolusDatabaseId = extendedBolus.id
        end?.bolusDatabaseId = extendedBolus.id
        return extendedBolus.id
    }

    private fun saveMultiwaveBolusLink(start: Bolus?, end: Bolus?, standardBolusId: Long, extendedBolusId: Long): Long {
        val bolusId = (start?.bolusId ?: end!!.bolusId).toLong()
        var multiwaveBolusLink = when {
            start == null && end != null -> {
                AppRepository.database.multiwaveBolusLinkDao.findByPumpId_StartIdIsNotNull_EndIdIsNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
            }
            else -> {
                AppRepository.database.multiwaveBolusLinkDao.findByPumpId_StartAndEndIDsAreNull(InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT, pumpSerial, bolusId)
            }
        }
        if (multiwaveBolusLink != null) {
            if (start != null) multiwaveBolusLink.interfaceIDs.startId = start.eventId
            if (end != null) multiwaveBolusLink.interfaceIDs.endId = end.eventId
            AppRepository.database.multiwaveBolusLinkDao.updateExistingEntry(multiwaveBolusLink)
        } else {
            multiwaveBolusLink = MultiwaveBolusLink(
                    bolusID = standardBolusId,
                    extendedBolusID = extendedBolusId
            )
            if (start != null) multiwaveBolusLink.interfaceIDs.startId = start.eventId
            if (end != null) multiwaveBolusLink.interfaceIDs.endId = end.eventId
            multiwaveBolusLink.interfaceIDs.pumpSerial = pumpSerial
            AppRepository.database.multiwaveBolusLinkDao.insertNewEntry(multiwaveBolusLink)
        }
        start?.multiwaveBolusLinkDatabaseId = multiwaveBolusLink.id
        end?.multiwaveBolusLinkDatabaseId = multiwaveBolusLink.id
        return multiwaveBolusLink.id
    }

    private fun processBoluses() {
        val groupedBoluses = boluses.groupBy { it.bolusId }
        val bolusPairs = groupedBoluses.map { entry -> entry.value.find { it.start } to entry.value.findLast { !it.start } }
        bolusPairs.forEach {
            val type = it.first?.type ?: it.second!!.type
            val bolusDatabaseId = if (type == Bolus.Type.STANDARD || type == Bolus.Type.MULTIWAVE) {
                saveStandardBolus(it.first, it.second)
            } else {
                null
            }
            val extendedBolusDatabaseId = if (type == Bolus.Type.EXTENDED || type == Bolus.Type.MULTIWAVE) {
                saveExtendedBolus(it.first, it.second)
            } else {
                null
            }
            if (type == Bolus.Type.MULTIWAVE) {
                saveMultiwaveBolusLink(it.first, it.second, bolusDatabaseId!!, extendedBolusDatabaseId!!)
            }
        }
    }

    data class TotalDailyDose(
            val eventId: Long,
            val timestamp: Long,
            val bolusAmount: Double,
            val basalAmount: Double,
            var databaseId: Long? = null
    )

    data class OperatingModeChange(
            val eventId: Long,
            val timestamp: Long,
            val from: OperatingMode,
            val to: OperatingMode,
            var therapyEventDatabaseId: Long? = null,
            var tbrDatabaseId: Long? = null
    ) {
        enum class OperatingMode {
            STARTED,
            STOPPED,
            PAUSED
        }
    }

    data class TherapyEvent(
            val eventId: Long,
            val timestamp: Long,
            val type: Type,
            var databaseId: Long? = null
    ) {
        enum class Type {
            CANNULA_FILLED,
            TUBE_FILLED,
            RESERVOIR_CHANGED,
            OCCLUSION,
            BATTERY_EMPTY,
            BATTERY_CHANGED,
            RESERVOIR_EMPTY
        }
    }

    data class TemporaryBasal(
            val start: Boolean,
            val eventId: Long,
            val timestamp: Long,
            val duration: Long,
            val percentage: Int,
            var databaseId: Long? = null
    )

    data class Bolus(
            val start: Boolean,
            val eventId: Long,
            val type: Type,
            val timestamp: Long,
            val bolusId: Int,
            val immediateAmount: Double,
            val duration: Long,
            val extendedAmount: Double,
            var bolusDatabaseId: Long? = null,
            var extendedBolusDatabaseId: Long? = null,
            var multiwaveBolusLinkDatabaseId: Long? = null
    ) {
        enum class Type {
            STANDARD,
            MULTIWAVE,
            EXTENDED
        }
    }

}