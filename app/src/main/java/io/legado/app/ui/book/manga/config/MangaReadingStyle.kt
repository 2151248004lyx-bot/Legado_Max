package io.legado.app.ui.book.manga.config

import com.google.gson.annotations.SerializedName

/** Stored as JSON in preferences / ReadConfig; all fields are immutable to book-source scripts. */
internal data class MangaReadingStyle(
    @SerializedName("mode") val mode: Int = CONTINUOUS,
    @SerializedName("layout") val layout: Int = VERTICAL,
    @SerializedName("rtl") val rtl: Boolean = false,
    @SerializedName("effect") val effect: Int = SLIDE,
) {
    val paged: Boolean get() = mode == PAGED
    val horizontal: Boolean get() = layout == HORIZONTAL

    fun normalized() = copy(mode = mode.coerceIn(PAGED, CONTINUOUS),
        layout = layout.coerceIn(HORIZONTAL, VERTICAL), effect = effect.coerceIn(NONE, CURL))

    companion object {
        const val PAGED = 0
        const val CONTINUOUS = 1
        const val HORIZONTAL = 0
        const val VERTICAL = 1
        const val NONE = 0
        const val SLIDE = 1
        const val COVER = 2
        const val CURL = 3
    }
}

/** Same slow-to-fast screen intervals as text auto-reading; independent from page delay. */
internal val mangaScrollIntervals = intArrayOf(120, 90, 60, 40, 30, 20, 10, 5, 1)
