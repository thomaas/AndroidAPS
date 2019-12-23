package info.nightscout.androidaps.networking.nightscout

import info.nightscout.androidaps.dependencyInjection.networking.NSRetrofitProvider
import info.nightscout.androidaps.networking.nightscout.responses.StatusResponse
import io.reactivex.Single

/**
 * Created by adrian on 2019-12-23.
 */

class NightscoutService(private val provider: NSRetrofitProvider) {

    fun status(): Single<StatusResponse> = provider.getNSService().status().map {
        // TODO: write generic mapper for success/Failure?
        when {
            it.isSuccessful -> it.body()
            else            -> TODO("not successful, bam, crash")
        }
    }

}