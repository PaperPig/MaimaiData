package com.paperpig.maimaidata.utils

import com.paperpig.maimaidata.MaimaiDataApplication
import io.github.whitemagic2014.tts.TTS
import io.github.whitemagic2014.tts.TTSVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object TtsManager {
    private val voice by lazy {
        TTSVoice.provides().firstOrNull { it.shortName == Constants.VOICE_NAME }
            ?: error("TTS voice not found: ${Constants.VOICE_NAME}")
    }

    private val mutex = Mutex()

    // 合成并缓存曲名语音，避免特殊曲名和并发请求损坏缓存。
    suspend fun transTTS(content: String): String =
        withContext(Dispatchers.IO) {
            require(content.isNotBlank()) { "TTS content must not be blank" }
            mutex.withLock {
                val application = MaimaiDataApplication.instance
                val fileDir = File(
                    application.externalCacheDir ?: application.cacheDir,
                    Constants.VOICE_SAVE_DIR
                )
                check(fileDir.isDirectory || fileDir.mkdirs()) { "Cannot create TTS cache directory" }

                // 缓存键包含音色，且不使用曲名作为路径，兼容斜杠和长曲名。
                val fileName = MessageDigest.getInstance("SHA-256")
                    .digest("${Constants.VOICE_NAME}\n$content".toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }
                val file = File(fileDir, "$fileName.mp3")
                if (file.isFile && file.length() > 0) {
                    return@withLock file.path
                }

                // 仅在合成成功后发布缓存，失败时清理临时文件，供后续点击重试。
                val tempFile = File.createTempFile("tts-", ".mp3", fileDir)
                try {
                    TTS(voice, content)
                        .findHeadHook()
                        .storage(fileDir.path)
                        .connectTimeout(10000)
                        .fileName(tempFile.nameWithoutExtension)
                        .overwrite(true)
                        .formatMp3()
                        .trans()
                    check(tempFile.length() > 0) { "TTS returned empty audio" }
                    check(tempFile.renameTo(file)) { "Cannot save TTS audio" }
                    file.path
                } finally {
                    tempFile.delete()
                }
            }
        }

}