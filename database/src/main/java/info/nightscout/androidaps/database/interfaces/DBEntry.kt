package info.nightscout.androidaps.database.interfaces

import info.nightscout.androidaps.database.embedments.InterfaceIDs

interface DBEntry {
    var id: Long
    var version: Int
    var lastModified: Long
    var valid: Boolean
    var referenceID: Long?
    var interfaceIDs2 : InterfaceIDs?

    val historic get() = referenceID != 0L

    val foreignKeysValid get() = referenceID != 0L

    var interfaceIDs: InterfaceIDs get() {
        if (interfaceIDs2 == null) interfaceIDs2 = InterfaceIDs()
        return interfaceIDs2!!
    } set(value) {
        interfaceIDs2 = value
    }
}