package com.paperpig.maimaidata.crawler

import com.paperpig.maimaidata.network.lxns.LxnsApiException
import com.paperpig.maimaidata.network.lxns.LxnsClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * 落雪查分器单个待抓取与上传的 NET 页面描述。
 */
data class LxnsPageTask(
    val url: String,
    val displayName: String,
    val expectScoreChanges: Boolean
)

/**
 * 落雪开发者 API HTML 上传器：复用已登录的微信 NET OkHttpClient 拉取指定页面并提交至落雪服务器。
 */
class LxnsHtmlUploader(
    private val lxnsClient: LxnsClient = LxnsClient.instance,
    private val logger: (String) -> Unit = { CrawlerCaller.writeLog(it) },
    private val throttleDelayMs: Long = DEFAULT_THROTTLE_DELAY_MS
) {

    @JvmOverloads
    fun uploadAll(
        netHttpClient: OkHttpClient,
        userToken: String,
        difficulties: Set<Int>,
        prefetchedFriendHtml: String? = null
    ) {
        if (userToken.isBlank()) {
            logger("落雪个人 API 密钥未配置，已跳过落雪上传")
            return
        }

        val tasks = buildPageTasks(difficulties)
        if (difficulties.none { it in STANDARD_DIFFICULTIES || it == UTAGE_DIFFICULTY }) {
            logger("落雪未选择任何成绩难度，将仅同步玩家信息与最近游玩记录")
        }

        var successCount = 0
        var totalChanges = 0

        for ((index, task) in tasks.withIndex()) {
            try {
                val html = if (!prefetchedFriendHtml.isNullOrBlank() &&
                    task.url.endsWith("friend/userFriendCode/")
                ) {
                    prefetchedFriendHtml
                } else {
                    logger("落雪 - 开始获取 ${task.displayName} 页面")
                    fetchHtmlWithRetry(netHttpClient, task.url, task.displayName)
                }
                logger("落雪 - ${task.displayName} 已获取，正在上传至落雪查分器")
                val changes = lxnsClient.uploadUserHtml(userToken, html)
                successCount++
                totalChanges += changes.size
                if (task.expectScoreChanges) {
                    logger("落雪 - ${task.displayName} 上传完成，新增/更新 ${changes.size} 条成绩")
                } else {
                    logger("落雪 - ${task.displayName} 上传完成")
                }
            } catch (e: LxnsApiException) {
                logger("落雪 - ${task.displayName} 上传失败：${e.message}")
                // 401/403/404 属于密钥或接口权限问题，无需继续尝试其余页面
                if (e.statusCode == 401 || e.statusCode == 403 || e.statusCode == 404) {
                    logger("落雪查分器更新已中止，请检查个人 API 密钥或接口权限")
                    return
                }
            } catch (e: Exception) {
                logger("落雪 - ${task.displayName} 同步失败：${e.message ?: e}")
            }

            if (index < tasks.lastIndex && throttleDelayMs > 0L) {
                try {
                    Thread.sleep(throttleDelayMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }

        logger("落雪查分器同步结束：成功 ${successCount}/${tasks.size} 页，共更新 $totalChanges 条成绩")
    }

    @Throws(IOException::class)
    private fun fetchHtmlWithRetry(
        netHttpClient: OkHttpClient,
        url: String,
        displayName: String
    ): String {
        var lastError: Exception? = null
        for (attempt in 1..MAX_FETCH_RETRIES) {
            try {
                val request = Request.Builder().url(url).get().build()
                netHttpClient.newCall(request).execute().use { response ->
                    val html = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}")
                    }
                    if (html.isBlank()) {
                        throw IOException("页面内容为空")
                    }
                    val finalPath = response.request.url.encodedPath
                    if (finalPath.contains("/error", ignoreCase = true)) {
                        throw IOException("舞萌 NET 返回错误页面，请重新登录重试")
                    }
                    return html
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_FETCH_RETRIES) {
                    logger("落雪 - 获取 $displayName 出错(${e.message})，进行第 $attempt 次重试")
                }
            }
        }
        throw IOException("获取 $displayName 页面失败：${lastError?.message}", lastError)
    }

    companion object {
        const val UTAGE_DIFFICULTY = 10
        private val STANDARD_DIFFICULTIES = 0..4
        private const val MAX_FETCH_RETRIES = 3
        private const val DEFAULT_THROTTLE_DELAY_MS = 250L

        private const val NET_BASE_URL = "https://maimai.wahlap.com/maimai-mobile/"

        private val DIFF_NAME_MAP = mapOf(
            0 to "Basic",
            1 to "Advance",
            2 to "Expert",
            3 to "Master",
            4 to "Re:Master"
        )

        /**
         * 过滤支持的难度列表（BASIC~Re:MASTER 以及宴会场 10）。
         */
        @JvmStatic
        fun filterSupportedDifficulties(difficulties: Set<Int>): Set<Int> {
            return difficulties.filter { it in STANDARD_DIFFICULTIES || it == UTAGE_DIFFICULTY }.toSet()
        }

        /**
         * 按落雪官方 HTML 上传接口要求生成待抓取的 NET 页面任务。
         */
        @JvmStatic
        fun buildPageTasks(difficulties: Set<Int>): List<LxnsPageTask> {
            val tasks = mutableListOf(
                LxnsPageTask(
                    url = "${NET_BASE_URL}friend/userFriendCode/",
                    displayName = "玩家信息",
                    expectScoreChanges = false
                ),
                LxnsPageTask(
                    url = "${NET_BASE_URL}record/",
                    displayName = "最近游玩记录",
                    expectScoreChanges = true
                )
            )

            difficulties
                .filter { it in STANDARD_DIFFICULTIES }
                .sorted()
                .forEach { diff ->
                    val diffName = DIFF_NAME_MAP[diff] ?: "难度$diff"
                    tasks.add(
                        LxnsPageTask(
                            url = "${NET_BASE_URL}record/musicSort/search/?search=V&sort=1&playCheck=on&diff=$diff",
                            displayName = "$diffName 最佳成绩",
                            expectScoreChanges = true
                        )
                    )
                }

            if (difficulties.contains(UTAGE_DIFFICULTY)) {
                tasks.add(
                    LxnsPageTask(
                        url = "${NET_BASE_URL}record/musicGenre/search/?genre=99&diff=$UTAGE_DIFFICULTY",
                        displayName = "宴会场",
                        expectScoreChanges = true
                    )
                )
            }

            return tasks
        }
    }
}
