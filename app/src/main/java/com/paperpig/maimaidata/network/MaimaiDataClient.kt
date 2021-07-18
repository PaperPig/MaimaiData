package com.paperpig.maimaidata.network

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
                    .build()
            )
            .build()
    }

    /**
     * get the service to request net interface
     */
    fun getService(): MaimaiDataService = retrofit.create(MaimaiDataService::class.java)
}