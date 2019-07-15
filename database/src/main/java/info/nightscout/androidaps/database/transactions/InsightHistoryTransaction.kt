package info.nightscout.androidaps.database.transactions

class InsightHistoryTransaction(val pumpSerial: String) {

    val therapyEvents = mutableListOf<TherapyEvent>()
    val temporaryBasals = mutableListOf<TemporaryBasal>()
    val boluses = mutableListOf<Bolus>()
    val totalDailyDoses = mutableListOf<TotalDailyDose>()
    val operatingModeChanges = mutableListOf<OperatingModeChange>()

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