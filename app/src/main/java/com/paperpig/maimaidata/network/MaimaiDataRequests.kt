package com.paperpig.maimaidata.network

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.paperpig.maimaidata.model.AppUpdateModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response


/**
 * @author BBS
 * @since  2021/5/13
 */
object MaimaiDataRequests {
    /**
     * [MaimaiDataService.login]
     */
    fun login(userName: String, password: String): Observable<Response<ResponseBody>> {
        val requestBody = "{\"username\": \"$userName\", \"password\": \"$password\"}"
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
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

    /**
     * fetch the version info for updating
     */
    fun fetchUpdateInfo(): Observable<AppUpdateModel> =
            MaimaiDataClient
                    .instance
                    .getService()
                    .getUpdateInfo()
                    .compose(MaimaiDataTransformer.handleResult())
                    .flatMap {
                        val model = Gson().fromJson(it, AppUpdateModel::class.java)
                        Observable.just(model)
                    }
}