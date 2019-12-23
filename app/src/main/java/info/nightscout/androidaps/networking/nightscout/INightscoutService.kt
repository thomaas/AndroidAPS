package info.nightscout.androidaps.networking.nightscout

import info.nightscout.androidaps.networking.nightscout.responses.StatusResponse
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET

/**
 * Created by adrian on 2019-12-23.
 */

interface INightscoutService {

    @GET("v3/status")
    fun status(): Single<Response<StatusResponse>>

}