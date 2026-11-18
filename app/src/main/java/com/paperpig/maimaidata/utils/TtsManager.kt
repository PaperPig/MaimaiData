package com.paperpig.maimaidata.utils

import android.util.Log
import com.paperpig.maimaidata.MaimaiDataApplication
import io.github.whitemagic2014.tts.EdgeGec
import io.github.whitemagic2014.tts.TTSVoice
import io.github.whitemagic2014.tts.bean.Voice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object TtsManager {
    private const val TAG = "TtsManager"
    private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
    private const val CHROMIUM_MAJOR_VERSION = "143"
    private const val SEC_MS_GEC_VERSION = "2-$CHROMIUM_FULL_VERSION"
    private const val EDGE_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR_VERSION.0.0.0 Safari/537.36 " +
            "Edg/$CHROMIUM_MAJOR_VERSION.0.0.0"
    private const val EDGE_ORIGIN = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold"
    private const val EDGE_WS_URL =
        "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
            "?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val EDGE_CLOCK_URL =
        "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/voices/list" +
            "?trustedclienttoken=6A5AA1D4EAFF4E9FB37E23D68491D6F4"

    private const val EPOCH_1601_MS = -11644473600000L
    private const val OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3"
    private const val MAX_ATTEMPTS = 3
    private const val ATTEMPT_TIMEOUT_MS = 10_000L
    private val RETRY_DELAYS_MS = longArrayOf(300L, 700L)
    private val AUDIO_HEADER_MARKER = "Path:audio\r\n".encodeUtf8()

    private val voice: Voice by lazy {
        TTSVoice.provides().firstOrNull { it.shortName == Constants.VOICE_NAME }
            ?: error("TTS voice not found: ${Constants.VOICE_NAME}")
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private val clockSyncMutex = Mutex()
    @Volatile
    private var clockSynced = false
    @Volatile
    private var clockSkewTicks = 0L

    /**
     * 合成并缓存曲名语音，使用 OkHttp WebSocket 并在握手或网络抖动时自动重试。
     */
    suspend fun transTTS(content: String): String = withContext(Dispatchers.IO) {
        require(content.isNotBlank()) { "TTS content must not be blank" }

        val application = MaimaiDataApplication.instance
        val fileDir = File(
            application.externalCacheDir ?: application.cacheDir,
            Constants.VOICE_SAVE_DIR
        )
        check(fileDir.isDirectory || fileDir.mkdirs()) { "Cannot create TTS cache directory" }

        val fileName = MessageDigest.getInstance("SHA-256")
            .digest("${Constants.VOICE_NAME}\n$content".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val cacheFile = File(fileDir, "$fileName.mp3")
        if (cacheFile.isFile && cacheFile.length() > 0) {
            return@withContext cacheFile.path
        }

        val escapedContent = escapeSsmlContent(content)
        syncClockIfNeeded(force = false)

        var lastError: Throwable? = null
        for (attempt in 1..MAX_ATTEMPTS) {
            val tempFile = File.createTempFile("tts-", ".mp3", fileDir)
            try {
                withTimeout(ATTEMPT_TIMEOUT_MS) {
                    synthesizeOnce(escapedContent, tempFile)
                }
                check(tempFile.length() > 0) { "TTS returned empty audio" }
                if (!tempFile.renameTo(cacheFile)) {
                    // 并发合成同一曲名时可能已由其他请求写入缓存，复用已落盘的缓存文件。
                    check(cacheFile.isFile && cacheFile.length() > 0) { "Cannot save TTS audio" }
                }
                return@withContext cacheFile.path
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "TTS attempt $attempt/$MAX_ATTEMPTS failed", e)
                if (attempt < MAX_ATTEMPTS) {
                    // 403 或时间偏差导致鉴权失败时强制重校准服务器时间后再重试。
                    if (e is TtsHttpException && e.code == 403) {
                        syncClockIfNeeded(force = true)
                    }
                    delay(RETRY_DELAYS_MS[(attempt - 1).coerceAtMost(RETRY_DELAYS_MS.lastIndex)])
                }
            } finally {
                tempFile.delete()
            }
        }
        throw lastError ?: IOException("TTS synthesis failed")
    }

    private suspend fun synthesizeOnce(escapedContent: String, outputFile: File) {
        val url = buildTtsUrl()
        val request = Request.Builder()
            .url(url)
            .header("Origin", EDGE_ORIGIN)
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", EDGE_UA)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        val dateStr = formatDate(Date(System.currentTimeMillis() + clockSkewTicks / 10_000L))
        val audioConfigMsg = buildAudioConfigMessage(dateStr)
        val ssmlMsg = buildSsmlMessage(dateStr, escapedContent)

        suspendCancellableCoroutine<Unit> { continuation ->
            val streamLock = Any()
            val stream = FileOutputStream(outputFile, false)
            var streamClosed = false
            var turnEnded = false

            fun closeStreamQuietly() {
                synchronized(streamLock) {
                    if (streamClosed) return
                    streamClosed = true
                    try {
                        stream.flush()
                    } catch (_: IOException) {
                    }
                    try {
                        stream.close()
                    } catch (_: IOException) {
                    }
                }
            }

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(audioConfigMsg)
                    webSocket.send(ssmlMsg)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("Path:turn.end")) {
                        turnEnded = true
                        closeStreamQuietly()
                        webSocket.close(1000, "done")
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val markerIndex = bytes.indexOf(AUDIO_HEADER_MARKER)
                    if (markerIndex < 0) return
                    val payloadStart = markerIndex + AUDIO_HEADER_MARKER.size
                    if (payloadStart >= bytes.size) return

                    try {
                        val raw = bytes.toByteArray()
                        synchronized(streamLock) {
                            if (!streamClosed) {
                                stream.write(raw, payloadStart, raw.size - payloadStart)
                            }
                        }
                    } catch (e: IOException) {
                        closeStreamQuietly()
                        webSocket.cancel()
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    closeStreamQuietly()
                    if (!turnEnded && continuation.isActive) {
                        continuation.resumeWithException(
                            IOException("TTS WebSocket closed unexpectedly ($code: $reason)")
                        )
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    closeStreamQuietly()
                    if (continuation.isActive) {
                        val error = if (response != null) {
                            TtsHttpException(response.code, "TTS handshake failed: HTTP ${response.code}", t)
                        } else {
                            t
                        }
                        continuation.resumeWithException(error)
                    }
                }
            }

            val webSocket = client.newWebSocket(request, listener)
            continuation.invokeOnCancellation {
                webSocket.cancel()
                closeStreamQuietly()
            }
        }
    }

    private suspend fun syncClockIfNeeded(force: Boolean) {
        if (clockSynced && !force) return
        clockSyncMutex.withLock {
            if (clockSynced && !force) return
            try {
                val request = Request.Builder().url(EDGE_CLOCK_URL).get().build()
                client.newCall(request).execute().use { response ->
                    val dateHeader = response.header("Date") ?: return@use
                    val serverTimeMs = DateTimeFormatter.RFC_1123_DATE_TIME
                        .parse(dateHeader, Instant::from)
                        .toEpochMilli()
                    clockSkewTicks = (serverTimeMs - System.currentTimeMillis()) * 10_000L
                    clockSynced = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync Edge TTS clock skew", e)
            }
        }
    }

    private fun buildTtsUrl(): String {
        val localTicks = (System.currentTimeMillis() - EPOCH_1601_MS) * 10_000L
        val token = EdgeGec.generateToken(localTicks + clockSkewTicks)
        return "$EDGE_WS_URL&Sec-MS-GEC=$token&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION"
    }

    private fun formatDate(date: Date): String =
        SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)", Locale.US).format(date)

    private fun buildAudioConfigMessage(dateStr: String): String =
        "X-Timestamp:$dateStr\r\n" +
            "Content-Type:application/json; charset=utf-8\r\n" +
            "Path:speech.config\r\n\r\n" +
            "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":" +
            "{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"true\"}," +
            "\"outputFormat\":\"$OUTPUT_FORMAT\"}}}}\n"

    private fun buildSsmlMessage(dateStr: String, escapedContent: String): String {
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='${voice.locale}'>" +
            "<voice name='${voice.name}'><prosody pitch='+0Hz' rate='+0%' volume='+0%'>" +
            "$escapedContent</prosody></voice></speak>"
        return "X-RequestId:$requestId\r\n" +
            "Content-Type:application/ssml+xml\r\n" +
            "X-Timestamp:${dateStr}Z\r\n" +
            "Path:ssml\r\n\r\n$ssml"
    }

    private fun escapeSsmlContent(input: String): String {
        val out = StringBuilder(input.length)
        for (ch in input) {
            val code = ch.code
            val isControl = code <= 8 || code in 11..12 || code in 14..31
            when {
                isControl -> out.append(' ')
                ch == '<' -> out.append("&lt;")
                ch == '>' -> out.append("&gt;")
                ch == '&' -> out.append("&amp;")
                ch == '"' -> out.append("&quot;")
                ch == '\'' -> out.append("&apos;")
                else -> out.append(ch)
            }
        }
        val escaped = out.toString()
        require(escaped.isNotBlank()) { "TTS content contains only blank/control characters" }
        return escaped
    }

    private class TtsHttpException(
        val code: Int,
        message: String,
        cause: Throwable? = null
    ) : IOException(message, cause)
}
