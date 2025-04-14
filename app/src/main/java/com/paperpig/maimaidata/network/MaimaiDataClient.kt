package com.paperpig.maimaidata.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author BBS
 * @since  2021-05-13
 */
class MaimaiDataClient private constructor() {

    companion object {
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            MaimaiDataClient()
        }

        const val BASE_URL = "https://www.diving-fish.com"
        const val IMAGE_BASE_URL = "https://maimaidx.jp/maimai-mobile/img/Music/"
        const val DIVING_FISH_COVER_URL = "https://www.diving-fish.com/covers/"

    }

    /**
     * the only retrofit object
     */
    private lateinit var retrofit: Retrofit

    /**
     * should init manually
     */
    fun init(customIp: String? = null) {
        val url = if (customIp.isNullOrBlank()) {
            BASE_URL
        } else {
            customIp
        }
        retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(url)
            .client(
                UnsafeOkHttpClient.unsafeOkHttpClient
                    .retryOnConnectionFailure(true)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        // interceptor: change url by header["urlName"]
                        val originalRequest: Request = chain.request()
                        val oldUrl: HttpUrl = originalRequest.url
                        val builder: Request.Builder = originalRequest.newBuilder()
                        val urlNameList: List<String> =
                            originalRequest.headers("urlName")
                        return@addInterceptor if (urlNameList.isNotEmpty()) {
                            builder.removeHeader("urlName")
                            val baseURL: HttpUrl = urlNameList[0].toHttpUrlOrNull()
                                ?: return@addInterceptor chain.proceed(originalRequest)
                            val newHttpUrl = oldUrl.newBuilder()
                                .scheme(baseURL.scheme)
                                .host(baseURL.host)
                                .port(baseURL.port)
                                .build()
                            val newRequest: Request = builder.url(newHttpUrl).build()
                            chain.proceed(newRequest)
                        } else {
                            chain.proceed(originalRequest)
                        }
                    }
                    .build()
            )
            .build()
    }

    /**
     * get the service to request net interface
     */
    fun getService(): MaimaiDataService = retrofit.create(MaimaiDataService::class.java)
}