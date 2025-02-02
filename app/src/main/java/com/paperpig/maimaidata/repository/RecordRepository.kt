package com.paperpig.maimaidata.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.paperpig.maimaidata.model.Record
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class RecordRepository {
    suspend fun saveRecord(context: Context, jsonElement: JsonElement) {
        withContext(Dispatchers.IO) {
            val output = context.openFileOutput("record.json", Context.MODE_PRIVATE)

            BufferedWriter(OutputStreamWriter(output)).use {
                it.write(jsonElement.toString())
            }
            RecordDataManager.loadData()
        }
    }

    suspend fun getRecord(context: Context): MutableList<Record> {
        return withContext(Dispatchers.IO) {
            try {
                val input = context.openFileInput("record.json")
                val list = input?.bufferedReader().use {
                    it?.readText()
                }
                Gson().fromJson(
                    list, object : TypeToken<MutableList<Record>>() {}.type
                )
            } catch (e: Exception) {
                mutableListOf()
            }
        }
    }
}