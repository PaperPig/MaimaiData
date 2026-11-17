package com.paperpig.maimaidata.network.lxns

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * 落雪咖啡屋 maimai DX 查分器个人 API 接口定义（使用 X-User-Token 鉴权）。
 */
interface LxnsApiService {

    @GET("/api/v0/user/maimai/player")
    fun getUserPlayer(
        @Header("X-User-Token") userToken: String
    ): Call<ResponseBody>

    @GET("/api/v0/user/maimai/player/scores")
    fun getUserScores(
        @Header("X-User-Token") userToken: String
    ): Call<ResponseBody>

    @Headers("Content-Type: text/plain; charset=utf-8")
    @POST("/api/v0/user/maimai/player/html")
    fun uploadUserHtml(
        @Header("X-User-Token") userToken: String,
        @Body html: RequestBody
    ): Call<ResponseBody>
}
