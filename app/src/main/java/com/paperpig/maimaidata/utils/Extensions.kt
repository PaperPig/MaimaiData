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
            return string == "maimai でらっくす"
        } else if (i == "舞萌DX 2021")
            return string == "maimai でらっくす Splash"
        else if (i == "舞萌DX 2022")
            return string == "maimai でらっくす Splash PLUS"
        else (string.contains(i))
        return true
    }
    return false
}