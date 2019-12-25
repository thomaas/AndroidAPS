package info.nightscout.androidaps.networking.nightscout

import info.nightscout.androidaps.dependencyInjection.networking.NSRetrofitProvider
import info.nightscout.androidaps.networking.nightscout.responses.StatusResponse
import io.reactivex.Single

/**
 * Created by adrian on 2019-12-23.
 */

class NightscoutService(private val provider: NSRetrofitProvider) {

    fun statusVerbose(): Single<StatusResponse> = provider.getNSService().statusVerbose().map {
        // TODO: write generic mapper for success/Failure?
        //  map to something used for user-feedback after settings-change?
        when {
            it.isSuccessful -> it.body()
            else            -> TODO("not successful, bam, crash")
        }
    }

    fun status() = provider.getNSService().statusSimple()

}