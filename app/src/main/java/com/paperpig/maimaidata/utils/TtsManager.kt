package com.paperpig.maimaidata.utils

import com.paperpig.maimaidata.MaimaiDataApplication
import io.github.whitemagic2014.tts.TTS
import io.github.whitemagic2014.tts.TTSVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object TtsManager {
    private val voice = TTSVoice.provides()
        .stream()
        .filter { v -> Constants.VOICE_NAME == v.shortName }
        .findFirst().get()

    private val fileDir = MaimaiDataApplication.instance.externalCacheDir?.path + Constants.VOICE_SAVE_DIR

    suspend fun transTTS(content: String): String =
        withContext(Dispatchers.IO) {
            val file = File("$fileDir$content.mp3")
            if (file.exists()) {
                return@withContext file.path
            }

            val fileName = TTS(voice, content)
                .findHeadHook()
                .storage(fileDir)
                .isRateLimited(true) // 在某些区域解决限速问题
                .fileName(content)  // 自定义文件名
                .overwrite(true)     // 同名时覆盖
                .formatMp3()    // 默认 mp3
                .trans()

            return@withContext fileDir + File.separator + fileName
        }

}