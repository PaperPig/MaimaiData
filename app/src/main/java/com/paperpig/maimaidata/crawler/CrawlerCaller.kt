package com.paperpig.maimaidata.crawler

import com.paperpig.maimaidata.network.vpn.core.LocalVpnService
import com.paperpig.maimaidata.utils.SpUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

object CrawlerCaller {
    private var listener: WechatCrawlerListener? = null

    fun getWechatAuthUrl(): String? {
        return try {
            val crawler = WechatCrawler()
            crawler.getWechatAuthUrl()
        } catch (error: IOException) {
            writeLog("获取微信登录url时出现错误:")
            onError(error)
            null
        }
    }

    @JvmStatic
    fun writeLog(text: String) {
        CoroutineScope(Dispatchers.Main).launch {
            listener?.onMessageReceived(text)
        }
    }

    @JvmStatic
    fun startAuth() {
        CoroutineScope(Dispatchers.Main).launch {
            listener?.onStartAuth()
        }
    }

    @JvmStatic
    fun finishUpdate() {
        CoroutineScope(Dispatchers.Main).launch {
            listener?.onFinishUpdate()
        }
    }

    @JvmStatic
    fun onError(e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
            listener?.onError(e)
        }
    }

    fun fetchData(authUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Thread.sleep(3000)
                LocalVpnService.IsRunning = false
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                onError(e)
            }
            try {
                val crawler = WechatCrawler()
                crawler.fetchAndUploadData(
                    SpUtil.getUserName(),
                    SpUtil.getPassword(),
                    getDifficulties(),
                    authUrl
                )
            } catch (e: IOException) {
                onError(e)
            }
        }
    }

    fun setOnWechatCrawlerListener(listener: WechatCrawlerListener) {
        this.listener = listener
    }

    fun removeOnWechatCrawlerListener() {
        this.listener = null
    }

    fun getDifficulties(): Set<Int> {
        return setOf(0, 1, 2, 3, 4)
    }
}