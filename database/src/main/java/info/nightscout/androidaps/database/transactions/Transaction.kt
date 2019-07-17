package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.interfaces.DBEntry

abstract class Transaction<T> {

    internal abstract fun run(): T

    internal val updated = mutableListOf<DBEntry>()

    internal val inserted = mutableListOf<DBEntry>()

}