package io.legado.app.ui.book.manga

internal data class MangaFooterSlotBounds(val x: Int, val width: Int)

/**
 * 中间留空时让长名称借用余宽，短时间/序号优先完整显示。
 * 中间有内容时保持几何居中；长中间项给两侧留下最多各三分之一的空间。
 */
internal fun mangaFooterSlotBounds(
    width: Int,
    leftNaturalWidth: Int,
    middleNaturalWidth: Int,
    rightNaturalWidth: Int,
    gap: Int,
): List<MangaFooterSlotBounds> {
    val left = leftNaturalWidth.coerceIn(0, width)
    val middle = middleNaturalWidth.coerceIn(0, width)
    val right = rightNaturalWidth.coerceIn(0, width)
    val spacing = gap.coerceIn(0, width / 2)
    if (middle == 0) {
        val available = width - if (left > 0 && right > 0) spacing else 0
        val leftWidth = minOf(left, available - minOf(right, available / 2))
        val rightWidth = minOf(right, available - leftWidth)
        return listOf(
            MangaFooterSlotBounds(0, leftWidth),
            MangaFooterSlotBounds(width / 2, 0),
            MangaFooterSlotBounds(width - rightWidth, rightWidth),
        )
    }
    val sideReservation = minOf(maxOf(left, right), (width - spacing * 2) / 3)
    val middleLimit = if (left == 0 && right == 0) width else {
        width - (sideReservation + spacing) * 2
    }
    val middleWidth = minOf(middle, middleLimit)
    val middleX = (width - middleWidth) / 2
    val separatingGap = if (middleWidth > 0) spacing else 0
    val leftWidth = minOf(left, (middleX - separatingGap).coerceAtLeast(0))
    val rightWidth = minOf(right, (width - middleX - middleWidth - separatingGap).coerceAtLeast(0))
    return listOf(
        MangaFooterSlotBounds(0, leftWidth),
        MangaFooterSlotBounds(middleX, middleWidth),
        MangaFooterSlotBounds(width - rightWidth, rightWidth),
    )
}
