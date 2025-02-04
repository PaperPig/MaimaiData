package com.paperpig.maimaidata.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.paperpig.maimaidata.model.ChartStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class ChartStatsRepository {

    suspend fun saveChartStats(context: Context, jsonElement: JsonElement) {
        withContext(Dispatchers.IO) {
            val output = context.openFileOutput("chart_stats.json", Context.MODE_PRIVATE)

            BufferedWriter(OutputStreamWriter(output)).use {
                it.write(jsonElement.toString())
            }
        }
    }

    suspend fun getChartStats(context: Context): Map<String, List<ChartStatus>> {
        return withContext(Dispatchers.IO) {
            try {
                val input = context.openFileInput("chart_stats.json")
                val list = input?.bufferedReader().use {
                    it?.readText()
                }
                Gson().fromJson(
                    list, object : TypeToken<Map<String, List<ChartStatus>>>() {}.type
                )
            } catch (e: Exception) {
                mapOf()
            }
        }
    }
}