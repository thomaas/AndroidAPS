package info.nightscout.androidaps.plugins.general.open_humans

import android.annotation.SuppressLint
import android.util.Base64
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
private val iso8601DateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

class OpenHumansAPI(
        val baseUrl: String,
        clientId: String,
        clientSecret: String,
        val redirectUri: String
) {

    private val authHeader = "Basic " + Base64.encodeToString("$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP)
    private val client = OkHttpClient()

    fun exchangeAuthToken(code: String) = sendTokenRequest("authorization_code", code)

    fun refreshToken(refreshToken: String) = sendTokenRequest("refreshToken", refreshToken)

    private fun sendTokenRequest(grantType: String, code: String) = Request.Builder()
            .url("$baseUrl/oauth2/token/")
            .addHeader("Authorization", authHeader)
            .post(FormBody.Builder()
                    .add("grant_type", grantType)
                    .add("redirect_uri", redirectUri)
                    .add("code", code)
                    .build())
            .build()
            .toSingle()
            .map {
                it.use {
                    val body = it.body()
                    val jsonObject = if (body != null) JSONObject(body.string()) else null
                    if (!it.isSuccessful) throw OHErrorneousResultException(it.code(), it.message(), jsonObject?.getString("error"))
                    OAuthTokens(
                            jsonObject!!.getString("access_token"),
                            jsonObject.getString("refresh_token"),
                            it.sentRequestAtMillis() + jsonObject.getInt("expires_in") * 1000L)
                }
            }

    fun getProjectMemberId(accessToken: String) = Request.Builder()
            .url("$baseUrl/api/direct-sharing/project/exchange-member/?access_token=$accessToken")
            .get()
            .build()
            .toSingle()
            .map { it.getJSONBody().getString("project_member_id") }

    fun prepareFileUpload(accessToken: String, fileName: String, metadata: FileMetadata) = Request.Builder()
            .url("$baseUrl/api/direct-sharing/project/files/upload/direct/?access_token=$accessToken")
            .post(FormBody.Builder()
                    .add("filename", fileName)
                    .add("metadata", metadata.toJSON().toString())
                    .build())
            .build()
            .toSingle()
            .map {
                val jsonObject = it.getJSONBody()
                PreparedUpload(
                        jsonObject.getString("id"),
                        jsonObject.getString("url")
                )
            }

    fun uploadFile(url: String, content: ByteArray): Completable = Request.Builder()
            .url(url)
            .put(RequestBody.create(null, content))
            .build()
            .toSingle()
            .doOnSuccess {
                it.use {
                    if (!it.isSuccessful) throw OHErrorneousResultException(it.code(), it.message(), null)
                }
            }
            .ignoreElement()

    fun completeFileUpload(accessToken: String, fileId: String): Completable = Request.Builder()
            .url("$baseUrl/api/direct-sharing/project/files/upload/complete/?access_token=$accessToken")
            .post(FormBody.Builder()
                    .add("file_id", fileId)
                    .build())
            .build()
            .toSingle()
            .doOnSuccess { it.getJSONBody() }
            .ignoreElement()

    private fun Response.getJSONBody() = use {
        val body = body()
        val jsonObject = if (body != null) JSONObject(body.string()) else null
        if (!isSuccessful) throw OHErrorneousResultException(code(), message(), jsonObject?.getString("detail"))
        jsonObject!!
    }

    private fun Request.toSingle() = Single.create<Response> {
        val call = client.newCall(this)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                it.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                it.onSuccess(response)
            }
        })
        it.setDisposable(Disposables.fromRunnable { call.cancel() })
    }

    class OHErrorneousResultException(val code: Int, val meaning: String, val detail: String?) : Exception("""$code ($meaning)${if (detail != null) ": $detail" else ""}""")

    data class OAuthTokens(
            val accessToken: String,
            val refreshToken: String,
            val expiresAt: Long
    )

    data class PreparedUpload(
            val fileId: String,
            val uploadURL: String
    )

    data class FileMetadata(
            val tags: List<String>,
            val description: String,
            val md5: String? = null,
            val creationDate: Long? = null,
            val startDate: Long? = null,
            val endDate: Long? = null
    ) {
        fun toJSON(): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("tags", JSONArray().apply { tags.forEach { put(it) } })
            jsonObject.put("description", description)
            jsonObject.put("md5", md5)
            creationDate?.let { jsonObject.put("creation_date", iso8601DateFormatter.format(Date(it))) }
            startDate?.let { jsonObject.put("start_date", iso8601DateFormatter.format(Date(it))) }
            endDate?.let { jsonObject.put("end_date", iso8601DateFormatter.format(Date(it))) }
            return jsonObject
        }
    }
}