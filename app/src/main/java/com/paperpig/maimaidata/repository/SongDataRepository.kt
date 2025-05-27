package com.paperpig.maimaidata.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paperpig.maimaidata.model.SongData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream

class SongDataRepository {
    suspend fun getData(context: Context?): List<SongData> {
        return withContext(Dispatchers.IO) {
            try {
                val fileInputStream: FileInputStream? =
                    context?.openFileInput("songdata.json")

                val list = fileInputStream?.bufferedReader().use {
                    it?.readText()
                }
                Gson().fromJson(
                    list, object : TypeToken<List<SongData>>() {}.type
                )

            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}