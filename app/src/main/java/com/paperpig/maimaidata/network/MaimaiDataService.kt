package com.paperpig.maimaidata.network

import com.google.gson.JsonElement
import io.reactivex.Observable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * @author BBS
 * @since  2021-05-13
 */
interface MaimaiDataService {
    /**
     * get maimai song detail list
     */
    @GET("/api/maimaidxprober/music_data")
    fun getSongList(): Observable<JsonElement>

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
}