package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class ResetDatabaseTransaction : Transaction<Unit>() {
    override fun run() {
        database.clearAllTables()
    }
}