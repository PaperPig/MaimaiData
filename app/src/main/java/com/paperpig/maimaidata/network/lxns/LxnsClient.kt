package com.paperpig.maimaidata.network.lxns

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * 落雪查分器 HTTP 客户端（个人 API），负责请求构建、429 速率限制退避与响应解析。
 */
class LxnsClient(
    baseUrl: String = BASE_URL,
    okHttpClient: OkHttpClient = defaultOkHttpClient(),
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) }
) {

    private val gson = Gson()
    private val service: LxnsApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()
        .create(LxnsApiService::class.java)

    /**
     * 通过个人 API 密钥查询当前授权玩家信息。
     */
    @Throws(IOException::class)
    fun fetchUserPlayer(userToken: String): LxnsPlayer {
        val type = object : TypeToken<LxnsResponse<LxnsPlayer>>() {}.type
        val result: LxnsResponse<LxnsPlayer> = executeWithRetry(
            callFactory = { service.getUserPlayer(userToken.trim()) },
            responseType = type
        )
        return result.data ?: throw LxnsApiException(result.code, result.message ?: "未返回玩家信息")
    }

    /**
     * 通过个人 API 向落雪提交单个页面的完整 NET HTML 源码。
     * 上传玩家信息或收藏品页面时服务端可能返回空 body 或无 data 字段，此时返回空列表。
     */
    @Throws(IOException::class)
    fun uploadUserHtml(userToken: String, html: String): List<LxnsScoreChange> {
        val body = html.toRequestBody(MEDIA_TYPE_PLAIN_TEXT)
        val type = object : TypeToken<LxnsResponse<List<LxnsScoreChange>>>() {}.type
        val result: LxnsResponse<List<LxnsScoreChange>>? = executeOptionalWithRetry(
            callFactory = { service.uploadUserHtml(userToken.trim(), body) },
            responseType = type
        )
        return result?.data ?: emptyList()
    }

    private fun <T> executeWithRetry(
        callFactory: () -> Call<ResponseBody>,
        responseType: Type
    ): LxnsResponse<T> {
        return executeOptionalWithRetry(callFactory, responseType)
            ?: throw IOException("落雪服务器返回空响应")
    }

    private fun <T> executeOptionalWithRetry(
        callFactory: () -> Call<ResponseBody>,
        responseType: Type
    ): LxnsResponse<T>? {
        var attempt = 0
        while (true) {
            val response: Response<ResponseBody> = callFactory().execute()
            if (response.code() == 429 && attempt < MAX_RATE_LIMIT_RETRIES) {
                val waitMs = parseRetryAfterMs(response) ?: (INITIAL_RETRY_DELAY_MS shl attempt)
                response.errorBody()?.close()
                response.body()?.close()
                try {
                    sleeper(waitMs)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("请求重试被中断", e)
                }
                attempt++
                continue
            }

            if (!response.isSuccessful) {
                val errorText = response.errorBody()?.string()
                val parsedMessage = parseErrorMessage(errorText)
                throw LxnsApiException(response.code(), parsedMessage)
            }

            val rawBody = response.body()?.string()?.trim().orEmpty()
            if (rawBody.isEmpty()) {
                return null
            }

            val parsed = gson.fromJson<LxnsResponse<T>>(rawBody, responseType)
            if (parsed != null && !parsed.success && parsed.code >= 400) {
                throw LxnsApiException(parsed.code, parsed.message)
            }
            return parsed
        }
    }

    private fun parseRetryAfterMs(response: Response<ResponseBody>): Long? {
        val headerValue = response.headers()["Retry-After"]?.trim()?.toLongOrNull() ?: return null
        return (headerValue.coerceIn(1L, 30L)) * 1000L
    }

    private fun parseErrorMessage(rawErrorBody: String?): String? {
        if (rawErrorBody.isNullOrBlank()) return null
        return try {
            val parsed = gson.fromJson(rawErrorBody, LxnsResponse::class.java)
            parsed?.message?.takeIf { it.isNotBlank() } ?: rawErrorBody
        } catch (_: Exception) {
            rawErrorBody
        }
    }

    companion object {
        const val BASE_URL = "https://maimai.lxns.net"
        private const val MAX_RATE_LIMIT_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private val MEDIA_TYPE_PLAIN_TEXT = "text/plain; charset=utf-8".toMediaType()

        val instance: LxnsClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            LxnsClient()
        }

        private fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
