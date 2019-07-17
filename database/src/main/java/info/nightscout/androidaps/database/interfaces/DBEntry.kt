package info.nightscout.androidaps.database.interfaces

import info.nightscout.androidaps.database.embedments.InterfaceIDs

interface DBEntry<T> {
    var id: Long
    var version: Int
    var lastModified: Long
    var valid: Boolean
    var referenceId: Long?
    var interfaceIDs_backing: InterfaceIDs?

    val historic: Boolean get() = referenceId != 0L

    val foreignKeysValid: Boolean get() = referenceId != 0L

    var interfaceIDs: InterfaceIDs
        get() {
            var value = this.interfaceIDs_backing
            if (value == null) {
                value = InterfaceIDs()
                interfaceIDs_backing = value
            }
            return value
        }
        set(value) {
            interfaceIDs_backing = value
        }

    fun contentEqualsTo(other: T) : Boolean = this == other
}