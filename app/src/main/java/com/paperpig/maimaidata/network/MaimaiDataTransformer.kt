package com.paperpig.maimaidata.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


/**
 * @author BBS
 * @since  2021-05-13
 */
class MaimaiDataTransformer {
    companion object {
        /**
         * switch thread & check err code
         */
        fun handleResult(): ObservableTransformer<JsonElement, JsonElement> {
            return ObservableTransformer { upstream ->
                upstream
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
            }
        }
    }
}