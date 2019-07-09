package info.nightscout.androidaps.utils

import info.nightscout.androidaps.Constants
import info.nightscout.androidaps.database.entities.GlucoseValue

fun GlucoseValue.TrendArrow.toSymbol(): String {
    return when (this) {
        GlucoseValue.TrendArrow.DOUBLE_DOWN -> "\u21ca"
        GlucoseValue.TrendArrow.SINGLE_DOWN -> "\u2193"
        GlucoseValue.TrendArrow.FORTY_FIVE_DOWN -> "\u2198"
        GlucoseValue.TrendArrow.FLAT -> "\u2192"
        GlucoseValue.TrendArrow.FORTY_FIVE_UP -> "\u2197"
        GlucoseValue.TrendArrow.SINGLE_UP -> "\u2191"
        GlucoseValue.TrendArrow.DOUBLE_UP -> "\u21c8"
        else -> "??"
    }
}

fun valueToUnits(value: Double, units: String): Double? {
    return if (units == Constants.MGDL)
        value
    else
        value * Constants.MGDL_TO_MMOLL
}

fun valueToUnitsToString(value: Double, units: String): String {
    return if (units == Constants.MGDL)
        DecimalFormatter.to0Decimal(value)
    else
        DecimalFormatter.to1Decimal(value * Constants.MGDL_TO_MMOLL)
}