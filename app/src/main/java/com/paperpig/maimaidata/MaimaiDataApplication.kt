package com.paperpig.maimaidata

import android.app.Application
import android.content.Context
import com.paperpig.maimaidata.network.MaimaiDataClient

/**
 * @author BBS
 * @since  2021/5/13
 */
class MaimaiDataApplication : Application() {
    companion object {
        lateinit var instance: MaimaiDataApplication
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
        MaimaiDataClient.instance.init()
    }
}