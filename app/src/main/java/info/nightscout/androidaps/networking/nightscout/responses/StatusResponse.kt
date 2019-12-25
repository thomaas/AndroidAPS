package info.nightscout.androidaps.networking.nightscout.responses

import com.google.gson.annotations.SerializedName

/**
 * Created by adrian on 2019-12-23.
 */

data class StatusResponse(
    @SerializedName("version") val version: String,
    @SerializedName("apiVersion") val apiVersion: String,
    @SerializedName("srvDate") val srvDate: Long,
    @SerializedName("storage") val storage: Storage,
    @SerializedName("apiPermissions") val apiPermissions: ApiPermissions
)

data class Storage(
    @SerializedName("storage") val storage: String,
    @SerializedName("version") val version: String
)

data class ApiPermissions(
    @SerializedName("devicestatus") val devicestatus: String,
    @SerializedName("entries") val entries: String,
    @SerializedName("food") val food: String,
    @SerializedName("profile") val profile: String,
    @SerializedName("settings") val settings: String,
    @SerializedName("treatments") val treatments: String
)