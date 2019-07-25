package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppDatabase
import info.nightscout.androidaps.database.interfaces.DBEntry

abstract class Transaction<T> {

    internal abstract fun run(): T

    internal val changes = mutableListOf<DBEntry>()

    internal lateinit var database: AppDatabase

}