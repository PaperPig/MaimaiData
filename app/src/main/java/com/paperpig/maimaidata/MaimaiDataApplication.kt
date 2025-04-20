package com.paperpig.maimaidata

import android.app.Application
import android.content.Context
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.model.MaxNotesStats
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.widgets.Settings

/**
 * @author BBS
 * @since  2021/5/13
 */
class MaimaiDataApplication : Application() {
    companion object {
        lateinit var instance: MaimaiDataApplication
    }

    var maxNotesStats: MaxNotesStats? = null

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
        MaimaiDataClient.instance.init()
    }

    override fun onCreate() {
        super.onCreate()

        Settings.init(this)


        AppDataBase.init(this)
    }
}