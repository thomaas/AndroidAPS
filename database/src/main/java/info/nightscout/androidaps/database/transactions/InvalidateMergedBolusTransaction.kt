package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class InvalidateMergedBolusTransaction(val mergedBolus: MergedBolus) : Transaction<Unit>() {
    override fun run() {
        mergedBolus.bolus?.let {
            val bolus = AppRepository.database.bolusDao.findById(it.id)
                    ?: throw IllegalArgumentException("There is no such Bolus with the specified ID.")
            bolus.valid = false
            AppRepository.database.bolusDao.updateExistingEntry(bolus)
            changes.add(bolus)
        }
        mergedBolus.carbs?.let {
            val carbs = AppRepository.database.carbsDao.findById(it.id)
                    ?: throw IllegalArgumentException("There are no such Carbs with the specified ID.")
            carbs.valid = false
            AppRepository.database.carbsDao.updateExistingEntry(carbs)
            changes.add(carbs)
        }
        mergedBolus.mealLink?.let {
            val mealLink = AppRepository.database.mealLinkDao.findById(it.id)
                    ?: throw IllegalArgumentException("There is no such MealLink with the specified ID.")
            mealLink.valid = false
            AppRepository.database.mealLinkDao.updateExistingEntry(mealLink)
            changes.add(mealLink)
        }
        mergedBolus.bolusCalculatorResult?.let {
            val bolusCalculatorResult = AppRepository.database.bolusCalculatorResultDao.findById(it.id)
                    ?: throw IllegalArgumentException("There is no such BolusCalculatorResult with the specified ID.")
            bolusCalculatorResult.valid = false
            AppRepository.database.bolusCalculatorResultDao.updateExistingEntry(bolusCalculatorResult)
            changes.add(bolusCalculatorResult)
        }
    }
}