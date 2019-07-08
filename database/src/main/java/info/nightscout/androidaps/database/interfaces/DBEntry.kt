package info.nightscout.androidaps.database.interfaces

import info.nightscout.androidaps.database.embedments.InterfaceIDs

interface DBEntry<T> {
    var id: Long
    var version: Int
    var lastModified: Long
    var valid: Boolean
    var referenceID: Long?
    var interfaceIDs2: InterfaceIDs?

    val historic: Boolean get() = referenceID != 0L

    val foreignKeysValid: Boolean get() = referenceID != 0L

    var interfaceIDs: InterfaceIDs
        get() {
            var value = this.interfaceIDs2
            if (value == null) {
                value = InterfaceIDs()
                interfaceIDs2 = value
            }
            return value
        }
        set(value) {
            interfaceIDs2 = value
        }

    fun contentEqualsTo(other: T) : Boolean = this == other
}