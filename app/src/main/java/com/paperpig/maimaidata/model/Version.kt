package com.paperpig.maimaidata.model

import androidx.annotation.DrawableRes

data class Version(
    /**
     * 版本名称
     */
    val versionName: String,

    /**
     * 用于显示版本图片的drawable资源
     */
    @DrawableRes val res: Int
)
