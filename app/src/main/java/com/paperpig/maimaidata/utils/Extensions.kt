package com.paperpig.maimaidata.utils

fun String.getInt(): Int {
    return if (this.isEmpty()) {
        0
    } else {
        this.toInt()
    }
}

fun List<String>.containsStr(string: String): Boolean {
    for (i in this) {
        if (i == "maimai") {
            if (string == i) return true
        } else if(string.contains(i))
            return true
    }
    return false
}