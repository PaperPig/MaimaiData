package com.paperpig.maimaidata.network

import com.google.gson.JsonElement
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response


/**
 * @author shenjiayi@didiglobal.com
 * @since  2021/5/13
 */
object MaimaiDataRequests {
    /**
     * [MaimaiDataService.login]
     */
    fun login(userName: String, password: String): Observable<Response<ResponseBody>> {
        val requestBody = RequestBody.create(
            MediaType.parse("Content-Type, application/json"),
            "{\"username\": \"$userName\", \"password\": \"$password\"}"
        )
        return MaimaiDataClient
            .instance
            .getService()
            .login(requestBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * [MaimaiDataService.getRecords]
     */
    fun getRecords(cookie: String): Observable<JsonElement> =
        MaimaiDataClient
            .instance
            .getService()
            .getRecords(cookie)
            .compose(MaimaiDataTransformer.handleResult())
}