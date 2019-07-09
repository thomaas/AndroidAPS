package info.nightscout.androidaps.database.embedments

data class InsulinConfiguration(
        var insulinLabel: String,
        var insulinEndTime: Int,
        var peak: Int
)