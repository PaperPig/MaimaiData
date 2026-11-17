package com.paperpig.maimaidata.network.lxns

import com.google.gson.annotations.SerializedName
import java.io.IOException

/**
 * 落雪查分器 API 统一外层响应结构。
 */
data class LxnsResponse<T>(
    val success: Boolean = false,
    val code: Int = 0,
    val message: String? = null,
    val data: T? = null
)

/**
 * 落雪玩家基本信息。
 */
data class LxnsPlayer(
    val name: String = "",
    val rating: Int = 0,
    @SerializedName("friend_code")
    val friendCode: Long = 0L
)

/**
 * 上传成绩或 HTML 页面后返回的单个成绩变化项。
 */
data class LxnsScoreChange(
    val id: Int = 0,
    @SerializedName("song_name")
    val songName: String = "",
    val level: String = "",
    @SerializedName("level_index")
    val levelIndex: Int = 0,
    val type: String = "",
    val achievements: LxnsChangeDetail<Double>? = null,
    val fc: LxnsChangeDetail<String>? = null,
    val fs: LxnsChangeDetail<String>? = null,
    @SerializedName("dx_score")
    val dxScore: LxnsChangeDetail<Int>? = null,
    @SerializedName("dx_rating")
    val dxRating: LxnsChangeDetail<Double>? = null
)

/**
 * 单个字段的更新前后数值变化。
 */
data class LxnsChangeDetail<T>(
    val old: T? = null,
    val new: T? = null
)

/**
 * 落雪 API 请求异常，包含 HTTP 状态码和服务端提示信息。
 */
class LxnsApiException(
    val statusCode: Int,
    val apiMessage: String?
) : IOException(buildReadableMessage(statusCode, apiMessage)) {

    companion object {
        fun buildReadableMessage(statusCode: Int, apiMessage: String?): String {
            val detail = apiMessage?.takeIf { it.isNotBlank() }
            return when (statusCode) {
                401 ->
                    "落雪个人 API 密钥无效或已过期（401）${detail?.let { ": $it" } ?: ""}"
                403 ->
                    "落雪个人 API 权限拒绝（403）${detail?.let { ": $it" } ?: ""}"
                404 -> when {
                    detail?.contains("player", ignoreCase = true) == true ->
                        "未找到落雪玩家档案（404: $detail）：请先在落雪网页同步一次数据"
                    else ->
                        "落雪接口返回 404（${detail ?: "Not Found"}）：该接口可能未对个人 API 开放或尚未建档"
                }
                429 ->
                    "落雪接口请求过于频繁（429），请稍后再试"
                400 ->
                    "请求参数或 HTML 格式有误（400）${detail?.let { ": $it" } ?: ""}"
                else ->
                    "落雪接口请求失败（HTTP $statusCode）${detail?.let { ": $it" } ?: ""}"
            }
        }
    }
}
