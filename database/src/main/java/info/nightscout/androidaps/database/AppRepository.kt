package info.nightscout.androidaps.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import info.nightscout.androidaps.database.entities.GlucoseValue
import io.reactivex.CompletableObserver
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

object AppRepository {

    private const val DB_FILE = "AndroidAPS.db"

    private lateinit var database: AppDatabase;

    fun initialize(context: Context) {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DB_FILE).build()
    }
}