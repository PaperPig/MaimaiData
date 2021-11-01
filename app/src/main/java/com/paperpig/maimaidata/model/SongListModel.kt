package com.paperpig.maimaidata.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongListModel {
    suspend fun getData(context: Context?): List<SongData> {
        return withContext(Dispatchers.IO) {
            val list = context?.assets?.open("music_data.json")?.bufferedReader()
                .use { it?.readText() }
            Gson().fromJson(
                list, object : TypeToken<List<SongData>>() {}.type
            )
        }
    }
}