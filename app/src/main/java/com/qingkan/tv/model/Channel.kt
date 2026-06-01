package com.qingkan.tv.model

data class Channel(
    val id: Int,
    val name: String,
    val url: String,
    val logo: String = ""
)
