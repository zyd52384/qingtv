package com.qingkan.tv.model

data class Channel(
    val id: Int,
    val name: String,
    val url: String,
    val group: String = "CCTV"  // CCTV / 卫视 / 数字
)
