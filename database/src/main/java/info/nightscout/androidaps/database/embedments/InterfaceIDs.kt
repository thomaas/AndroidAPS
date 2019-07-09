package info.nightscout.androidaps.database.embedments

data class InterfaceIDs(
        var nightscoutSystemId: String? = null,
        var nightscoutId: String? = null,
        var pumpType: String? = null,
        var pumpSerial: String? = null,
        var pumpId: Long? = null,
        var startId: Long? = null,
        var endId: Long? = null
)