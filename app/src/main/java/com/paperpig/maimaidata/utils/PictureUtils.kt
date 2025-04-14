package com.paperpig.maimaidata.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.paperpig.maimaidata.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PictureUtils {
    private const val IMAGE_ROOT_DIR = "maimaidata_image"
    private const val COVER_SUB_DIR = "cover"

    private const val SHARED_IMAGE_FILENAME = "shared_images.png"

    val imagePath = "${Environment.DIRECTORY_PICTURES}/$IMAGE_ROOT_DIR/"
    val coverPath = "$imagePath$COVER_SUB_DIR/"


    suspend fun savePicture(context: Context, bitmap: Bitmap, filePath: String, fileName: String) {
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                //检查是否已存在相同路径和文件名的图片
                val selection =
                    "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?"
                val selectionArgs = arrayOf(filePath, "${fileName}.png")

                val cursor = context.contentResolver.query(
                    imageCollection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    selection,
                    selectionArgs,
                    null
                )

                val fileExists = cursor?.use { it.moveToFirst() } == true

                if (fileExists) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.file_saved_to_path, filePath),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@withContext
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, filePath)
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )

                context.contentResolver.openOutputStream(uri!!).use {
                    try {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it!!)
                        it.apply {
                            flush()
                            close()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.file_saved_to_path, filePath),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            context, R.string.image_generation_error, Toast.LENGTH_SHORT
                        ).show()
                    }
                }


            } else {
                val outputDir = Environment.getExternalStoragePublicDirectory(filePath)
                val imageFile = File(
                    outputDir, "${fileName}.png"
                )
                if (!outputDir.exists()) outputDir.mkdirs()
                try {
                    FileOutputStream(imageFile).use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, it)
                        it.flush()
                        it.close()
                        MediaScannerConnection.scanFile(
                            context, arrayOf(imageFile.path), null, null
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.file_saved_to_path, filePath),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        context, R.string.image_generation_error, Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
    }

    /**
     * 保存临时文件
     */
    suspend fun saveCacheBitmap(context: Context, bitmap: Bitmap): File? {
        return withContext(Dispatchers.IO) {
            val cachePath = File(context.externalCacheDir, SHARED_IMAGE_FILENAME)
            try {
                FileOutputStream(cachePath).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                cachePath
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }
}