package com.paperpig.maimaidata.repository

import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.model.Record

object RecordDataManager {
    var list = mutableListOf<Record>()

    suspend fun loadData() {
        list = RecordRepository().getRecord(MaimaiDataApplication.instance)
    }
}