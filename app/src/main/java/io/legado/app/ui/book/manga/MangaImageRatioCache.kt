package io.legado.app.ui.book.manga

/** 阅读会话内的图片几何，不随 LazyList item 回收；不保存 Bitmap 或写入书籍配置。 */
internal class MangaImageRatioCache(private val capacity: Int = 256) {
    private val ratios = LinkedHashMap<String, Float>(16, 0.75f, true)

    @Synchronized
    fun get(url: String): Float = ratios[url] ?: 0f

    @Synchronized
    fun put(url: String, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        ratios[url] = width.toFloat() / height
        while (ratios.size > capacity) ratios.remove(ratios.keys.first())
    }
}
