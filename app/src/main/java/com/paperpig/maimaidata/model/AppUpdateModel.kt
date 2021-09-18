package com.paperpig.maimaidata.model

import com.google.gson.annotations.SerializedName

/**
 * @author BBS
 * @since  2021/9/6
 */
data class AppUpdateModel(
    /**
     * version string
     */
    @SerializedName("emperor_version")
    var version: String? = null,

    /**
     * newest apk url
     */
    @SerializedName("emperor_url")
    var url: String? = null
)