package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.Bolus
import info.nightscout.androidaps.database.entities.BolusCalculatorResult
import info.nightscout.androidaps.database.entities.Carbs
import info.nightscout.androidaps.database.entities.links.MealLink
import java.util.*

class MealBolusTransaction(
        val timestamp: Long,
        val insulin: Double,
        val carbs: Double,
        val smb: Boolean,
        val carbTime: Long = 0,
        val bolusCalculatorResult: BolusCalculatorResult? = null
) : Transaction<Unit>() {

    override fun run() {
        var entries = 0
        val utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong()
        val bolusDBId = if (insulin > 0) {
            entries += 1
            AppRepository.database.bolusDao.insertNewEntry(Bolus(
                    timestamp = timestamp,
                    utcOffset = utcOffset,
                    amount = insulin,
                    type = if (smb) Bolus.Type.SMB else Bolus.Type.NORMAL,
                    basalInsulin = false
            ).apply {
                changes.add(this)
            })
        } else {
            null
        }
        val carbsDBId = if (carbs > 0) {
            entries += 1
            AppRepository.database.carbsDao.insertNewEntry(Carbs(
                    timestamp = timestamp + carbTime,
                    utcOffset = utcOffset,
                    amount = carbs,
                    duration = 0
            ).apply {
                changes.add(this)
            })
        } else {
            null
        }
        val bolusCalculatorResultDBId = if (bolusCalculatorResult != null) {
            entries += 1
            AppRepository.database.bolusCalculatorResultDao.insertNewEntry(BolusCalculatorResult(
                    timestamp = timestamp,
                    utcOffset = utcOffset,
                    targetBGLow = bolusCalculatorResult.targetBGLow,
                    targetBGHigh = bolusCalculatorResult.targetBGHigh,
                    isf = bolusCalculatorResult.isf,
                    ic = bolusCalculatorResult.ic,
                    bolusIOB = bolusCalculatorResult.bolusIOB,
                    bolusIOBUsed = bolusCalculatorResult.bolusIOBUsed,
                    basalIOB = bolusCalculatorResult.basalIOB,
                    basalIOBUsed = bolusCalculatorResult.basalIOBUsed,
                    glucoseValue = bolusCalculatorResult.glucoseValue,
                    glucoseUsed = bolusCalculatorResult.glucoseUsed,
                    glucoseDifference = bolusCalculatorResult.glucoseDifference,
                    glucoseInsulin = bolusCalculatorResult.glucoseInsulin,
                    glucoseTrend = bolusCalculatorResult.glucoseTrend,
                    trendUsed = bolusCalculatorResult.trendUsed,
                    trendInsulin = bolusCalculatorResult.trendInsulin,
                    carbs = bolusCalculatorResult.carbs,
                    carbsUsed = bolusCalculatorResult.carbsUsed,
                    carbsInsulin = bolusCalculatorResult.carbsInsulin,
                    otherCorrection = bolusCalculatorResult.otherCorrection,
                    totalInsulin = bolusCalculatorResult.totalInsulin
            ).apply {
                changes.add(this)
            })
        } else {
            null
        }
        if (entries > 1) {
            AppRepository.database.mealLinkDao.insertNewEntry(MealLink(
                    bolusId = bolusDBId,
                    carbsId = carbsDBId,
                    bolusCalcResultId = bolusCalculatorResultDBId
            ).apply {
                changes.add(this)
            })
        }
    }

    data class BolusCalculatorResult(
            var targetBGLow: Double,
            var targetBGHigh: Double,
            var isf: Double,
            var ic: Double,
            var bolusIOB: Double,
            var bolusIOBUsed: Boolean,
            var basalIOB: Double,
            var basalIOBUsed: Boolean,
            var glucoseValue: Double,
            var glucoseUsed: Boolean,
            var glucoseDifference: Double,
            var glucoseInsulin: Double,
            var glucoseTrend: Double,
            var trendUsed: Boolean,
            var trendInsulin: Double,
            var carbs: Double,
            var carbsUsed: Boolean,
            var carbsInsulin: Double,
            var otherCorrection: Double,
            var totalInsulin: Double
    )
}