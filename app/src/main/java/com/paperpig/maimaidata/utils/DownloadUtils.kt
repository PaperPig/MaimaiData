package com.paperpig.maimaidata.utils

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

object DownloadUtils {
    private const val TAG = "DownloadUtils"
    private const val BUFFER_SIZE = 16 * 1024
    private const val PROGRESS_INTERVAL_MS = 16L

    interface DownloadCallback {
        fun onStart() {}
        fun onProgress(downloaded: Long, total: Long, progress: Int) {}
        suspend fun onSuccess(file: File) {}
        fun onCancel() {}
        fun onError(e: Exception) {}
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 在指定协程作用域内发起下载，并将生命周期、进度与结果通过回调统一分发到主线程。
     */
    fun download(
        scope: CoroutineScope,
        url: String,
        targetFile: File,
        maxAttempts: Int = 3,
        callback: DownloadCallback
    ): Job {
        return scope.launch {
            try {
                withContext(Dispatchers.Main) { callback.onStart() }
                downloadFile(url, targetFile, maxAttempts) { downloaded, total, progress ->
                    callback.onProgress(downloaded, total, progress)
                }
                callback.onSuccess(targetFile)
            } catch (e: CancellationException) {
                withContext(NonCancellable + Dispatchers.Main) {
                    callback.onCancel()
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: $url", e)
                withContext(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }

    /**
     * 使用单连接 OkHttp 流式下载到临时文件并自动重试，校验完整后原子替换目标文件。
     */
    suspend fun downloadFile(
        url: String,
        targetFile: File,
        maxAttempts: Int = 3,
        onProgress: ((downloaded: Long, total: Long, progress: Int) -> Unit)? = null
    ) {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        val parentDir = targetFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }
        val tempFile = File(parentDir, "${targetFile.name}.tmp")
        var lastError: IOException? = null

        try {
            for (attempt in 1..maxAttempts) {
                try {
                    withContext(Dispatchers.IO) {
                        // 显式关闭 gzip 自动解压，确保服务端返回准确的 Content-Length 用于计算总大小与百分比。
                        val request = Request.Builder()
                            .url(url)
                            .header("Accept-Encoding", "identity")
                            .get()
                            .build()
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                throw IOException("Unexpected HTTP ${response.code}")
                            }
                            val body = response.body ?: throw IOException("Empty response body")
                            val contentLength = body.contentLength()
                            var totalRead = 0L
                            var lastNotifyTime = 0L

                            body.byteStream().use { input ->
                                tempFile.outputStream().use { output ->
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    while (true) {
                                        ensureActive()
                                        val read = input.read(buffer)
                                        if (read == -1) break
                                        output.write(buffer, 0, read)
                                        totalRead += read

                                        if (onProgress != null) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastNotifyTime >= PROGRESS_INTERVAL_MS ||
                                                (contentLength > 0L && totalRead >= contentLength)
                                            ) {
                                                lastNotifyTime = now
                                                val progress = if (contentLength > 0L) {
                                                    ((totalRead * 100L) / contentLength).toInt().coerceIn(0, 100)
                                                } else {
                                                    -1
                                                }
                                                withContext(Dispatchers.Main) {
                                                    onProgress(totalRead, contentLength, progress)
                                                }
                                            }
                                        }
                                    }
                                    output.flush()
                                }
                            }

                            if (totalRead <= 0L || (contentLength > 0L && totalRead != contentLength)) {
                                throw IOException("Incomplete download: $totalRead/$contentLength")
                            }
                            if (targetFile.exists() && !targetFile.delete()) {
                                tempFile.copyTo(targetFile, overwrite = true)
                            } else if (!tempFile.renameTo(targetFile)) {
                                tempFile.copyTo(targetFile, overwrite = true)
                            }
                        }
                    }
                    return
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    lastError = e
                    Log.w(TAG, "Download attempt $attempt/$maxAttempts failed: ${e.message}")
                    tempFile.delete()
                    if (attempt < maxAttempts) {
                        delay((1000L * attempt).milliseconds)
                    }
                }
            }
            throw lastError ?: IOException("Unknown download error")
        } finally {
            tempFile.delete()
        }
    }
}
