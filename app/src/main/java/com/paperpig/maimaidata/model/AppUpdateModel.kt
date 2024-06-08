package com.paperpig.maimaidata.model

import com.google.gson.annotations.SerializedName

/**
 * @author BBS
 * @since  2021/9/6
 */
data class AppUpdateModel(
    /**
     * apk version string
     */
    @SerializedName("apk_version")
    var version: String? = null,

    /**
     * newest apk url
     */
    @SerializedName("apk_url")
    var url: String? = null,

    /**
     * update info
     */
    @SerializedName("apk_info")
    var info: String? = null,

    /**
     * json data version string
     */
    @SerializedName("data_version_2")
    var dataVersion2: String? = null,

    /**
     * new json url
     */
    @SerializedName("data_url_2")
    var dataUrl2: String? = null
)