package io.legado.app.data.entities

import com.google.gson.annotations.SerializedName

/** 目录折叠仅需身份、顺序和分卷标记，不加载整本书的章节内容与变量。 */
data class CatalogOutlineEntry(
    @SerializedName("url") val url: String,
    @SerializedName("index") val index: Int,
    @SerializedName("isVolume") val isVolume: Boolean,
)
