package com.paperpig.maimaidata.utils

fun String.getInt(): Int {
    return if (this.isEmpty()) {
        0
    } else {
        this.toInt()
    }
}

fun List<String>.versionCheck(string: String): Boolean {
    for (i in this) {
        if (i == "maimai") {
            if (string == "maimai" || string == "maimai PLUS") return true
        } else if (i == "舞萌DX") {
            if (string == "maimai でらっくす") return true
        } else if (i == "舞萌DX 2021") {
            if (string == "maimai でらっくす Splash") return true
        } else if (i == "舞萌DX 2022") {
            if (string == "maimai でらっくす Splash PLUS") return true
        } else if(string.contains(i))
        return true
    }
    return false
}