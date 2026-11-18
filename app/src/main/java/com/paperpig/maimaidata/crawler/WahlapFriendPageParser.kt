package com.paperpig.maimaidata.crawler

import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.model.PlayerProfile
import com.paperpig.maimaidata.model.PlayerTrophy
import com.paperpig.maimaidata.repository.PlayerProfileRepository
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * 解析舞萌 NET `friend/userFriendCode/` 页面 HTML 为 App 通用 [PlayerProfile] 模型，
 * 并在已登录会话内将玩家头像下载缓存在本地私有目录，供离线/非落雪场景直接加载。
 */
object WahlapFriendPageParser {

    const val FRIEND_CODE_URL = "https://maimai.wahlap.com/maimai-mobile/friend/userFriendCode/"
    private const val NET_HOST_BASE = "https://maimai.wahlap.com"
    private const val AVATAR_FILE_PREFIX = "player_avatar_"

    private val FRIEND_CODE_REGEX = Regex(
        """class="[^"]*\btown_block\b[^"]*"[\s\S]*?<div[^>]*class="[^"]*\bsee_through_block\b[^"]*"[^>]*>\s*(\d+)\s*</div>"""
    )
    private val ICON_SRC_REGEX = Regex(
        """<img[^>]*src="((?:https?://[^"]+)?/maimai-mobile/img/Icon/[^"]+)"[^>]*>"""
    )
    private val TROPHY_COLOR_REGEX = Regex(
        """class="[^"]*\btrophy_block\s+trophy_([A-Za-z]+)[^"]*""""
    )
    private val TROPHY_NAME_REGEX = Regex(
        """class="[^"]*\btrophy_inner_block\b[^"]*"[^>]*>\s*<span>([\s\S]*?)</span>"""
    )
    private val NAME_REGEX = Regex(
        """<div[^>]*class="[^"]*\bname_block\b[^"]*"[^>]*>([\s\S]*?)</div>"""
    )
    private val RATING_REGEX = Regex(
        """<div[^>]*class="[^"]*\brating_block\b[^"]*"[^>]*>\s*(\d+)\s*</div>"""
    )
    private val COURSE_RANK_REGEX = Regex("""/img/course/course_rank_(\d{2})[^"]*\.png""")
    private val CLASS_RANK_REGEX = Regex("""/img/class/class_rank_s_(\d{2})[^"]*\.png""")
    private val STAR_REGEX = Regex("""icon_star\.png"[^>]*>\s*[×xX]\s*(\d+)""")
    private val NUMERIC_ENTITY_REGEX = Regex("""&#(x?[0-9a-fA-F]+);""")

    fun parse(html: String): PlayerProfile? {
        if (html.isBlank()) return null
        val name = NAME_REGEX.find(html)?.groupValues?.get(1)?.let(::unescapeHtml)?.trim().orEmpty()
        val friendCode = FRIEND_CODE_REGEX.find(html)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        if (name.isBlank() && friendCode == 0L) return null

        val rating = RATING_REGEX.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val courseRank = COURSE_RANK_REGEX.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val classRank = CLASS_RANK_REGEX.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val star = STAR_REGEX.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val trophyColor = TROPHY_COLOR_REGEX.find(html)?.groupValues?.get(1)?.trim()
        val trophyName = TROPHY_NAME_REGEX.find(html)?.groupValues?.get(1)?.let(::unescapeHtml)?.trim()
        val trophy = if (!trophyName.isNullOrBlank()) {
            PlayerTrophy(
                name = trophyName,
                color = trophyColor
            )
        } else {
            null
        }

        val rawIconSrc = ICON_SRC_REGEX.find(html)?.groupValues?.get(1)?.trim()
        val remoteIconUrl = rawIconSrc?.let { src ->
            if (src.startsWith("http")) src else "$NET_HOST_BASE$src"
        }

        return PlayerProfile(
            name = name,
            rating = rating,
            friendCode = friendCode,
            courseRank = courseRank,
            classRank = classRank,
            star = star,
            trophy = trophy,
            iconUrl = remoteIconUrl,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 解析 `friend/userFriendCode/` HTML 并将个人信息与本地缓存头像地址写入数据库。
     */
    @JvmStatic
    fun saveFromFriendHtml(netHttpClient: OkHttpClient, html: String) {
        val parsed = parse(html) ?: return
        val cachedIconUrl = parsed.iconUrl?.let { cacheAvatarLocally(netHttpClient, it) }
        val profileToSave = parsed.copy(iconUrl = cachedIconUrl ?: parsed.iconUrl)
        runCatching {
            runBlocking {
                PlayerProfileRepository.getInstance().saveProfile(profileToSave)
            }
        }
    }

    private fun cacheAvatarLocally(netHttpClient: OkHttpClient, remoteUrl: String): String? {
        return runCatching {
            val app = MaimaiDataApplication.instance
            val rawName = remoteUrl.substringAfterLast('/')
                .substringBefore('?')
                .substringBeforeLast('.')
                .replace(Regex("""[^A-Za-z0-9_-]"""), "")
                .ifBlank { "default" }
            val targetFile = File(app.filesDir, "$AVATAR_FILE_PREFIX$rawName.png")
            if (targetFile.exists() && targetFile.length() > 0L) {
                return@runCatching targetFile.toURI().toString()
            }

            val request = Request.Builder()
                .url(remoteUrl)
                .header("Referer", FRIEND_CODE_URL)
                .get()
                .build()

            netHttpClient.newCall(request).execute().use { response ->
                val contentType = response.header("Content-Type").orEmpty()
                val bytes = response.body?.bytes()
                if (!response.isSuccessful || bytes == null || bytes.isEmpty() ||
                    contentType.startsWith("text/html", ignoreCase = true)
                ) {
                    return@runCatching null
                }

                app.filesDir.listFiles { _, fileName ->
                    fileName.startsWith(AVATAR_FILE_PREFIX) && fileName != targetFile.name
                }?.forEach { it.delete() }

                targetFile.writeBytes(bytes)
                targetFile.toURI().toString()
            }
        }.getOrNull()
    }

    private fun unescapeHtml(raw: String): String {
        var result = raw
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
        result = NUMERIC_ENTITY_REGEX.replace(result) { match ->
            val token = match.groupValues[1]
            val code = if (token.startsWith("x", ignoreCase = true)) {
                token.drop(1).toIntOrNull(16)
            } else {
                token.toIntOrNull()
            }
            if (code != null && Character.isValidCodePoint(code)) {
                String(Character.toChars(code))
            } else {
                match.value
            }
        }
        return result
    }
}
