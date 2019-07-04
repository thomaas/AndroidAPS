package info.nightscout.androidaps.database.interfaces

import info.nightscout.androidaps.database.embedments.InterfaceIDs

interface DBEntry {
    var id: Long
    var version: Int
    var lastModified: Long
    var valid: Boolean
    var referenceID: Long
    var interfaceIDs : InterfaceIDs

    val historic get() = referenceID != 0L
}