package com.paperpig.maimaidata.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.paperpig.maimaidata.model.Record
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.lang.Exception

object MaimaiRecordUtils {
    fun saveRecord(context: Context, jsonElement: JsonElement) {
        val output = context.openFileOutput("record.json", Context.MODE_PRIVATE)

        BufferedWriter(OutputStreamWriter(output)).use {
            it.write(jsonElement.toString())
        }
    }

    fun getRecord(context: Context): List<Record>? {
        return try {
            val input = context.openFileInput("record.json")
            val list = input?.bufferedReader().use {
                it?.readText()
            }
            Gson().fromJson<List<Record>>(
                list, object : TypeToken<List<Record>>() {}.type
            )
        }catch (e:Exception){
            null
        }

    }
}