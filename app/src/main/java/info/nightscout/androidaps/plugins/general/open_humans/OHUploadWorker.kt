package info.nightscout.androidaps.plugins.general.open_humans

import android.content.Context
import android.net.wifi.WifiManager
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import io.reactivex.Single

class OHUploadWorker(
        val context: Context,
        workerParameters: WorkerParameters
) : RxWorker(context, workerParameters) {

    override fun createWork() = Single.defer {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager.isWifiEnabled && wifiManager.connectionInfo.networkId != -1)
            OpenHumansUploader.uploadData().andThen(Single.just(Result.success()))
        else Single.just(Result.retry())
    }
}