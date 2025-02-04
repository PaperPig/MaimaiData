package com.paperpig.maimaidata.network

import com.google.gson.JsonElement
import io.reactivex.Observable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author BBS
 * @since  2021-05-13
 */
interface MaimaiDataService {

    /**
     * get login info
     * set cookie: jwt_token
     * use jwt_token to get player's record
     *
     * @param body param json like {"username": string, "password": string}
     */
    @Headers("Content-Type:application/json;charset=UTF-8")
    @POST("/api/maimaidxprober/login")
    fun login(@Body body: RequestBody): Observable<Response<ResponseBody>>

    /**
     * player's record of songs
     * record on diving-fish.com manually
     */
    @GET("/api/maimaidxprober/player/records")
    fun getRecords(@Header("Cookie") cookie: String): Observable<JsonElement>

    /**
     * fetch update info from a noob's server
     */
    @Headers("urlName:https://bucket-1256206908.cos.ap-shanghai.myqcloud.com")
    @GET("/update.json")
    fun getUpdateInfo(): Observable<JsonElement>

    /**
     * get chart_status from diving-fish.com
     */
    @GET("/api/maimaidxprober/chart_stats")
    fun getChartStatus():Observable<JsonElement>
}