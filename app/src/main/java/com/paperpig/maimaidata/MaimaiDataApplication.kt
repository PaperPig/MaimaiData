package com.paperpig.maimaidata

import android.app.Application
import android.content.Context
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.repository.RecordDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    override fun onCreate() {
        super.onCreate()

        //启动时加载分数记录
        CoroutineScope(Dispatchers.IO).launch {
            RecordDataManager.loadData()
        }
    }
}