package com.paperpig.maimaidata.crawler

interface WechatCrawlerListener {
    fun onMessageReceived(logString: String)

    fun onStartAuth()

    fun onFinishUpdate()

    fun onError(e: Exception)
}