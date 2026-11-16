package io.legado.app.ui.book.read

import io.legado.app.data.entities.CatalogOutlineEntry

internal fun visibleCatalogRows(
    outline: List<CatalogOutlineEntry>,
    collapsedVolumes: Set<String>,
    descending: Boolean,
): List<CatalogOutlineEntry> {
    var hidden = false
    val visible = outline.filter { entry ->
        if (entry.isVolume) {
            hidden = entry.url in collapsedVolumes
            true
        } else {
            !hidden
        }
    }
    return if (descending) visible.asReversed() else visible
}

internal fun catalogVisiblePosition(rows: List<CatalogOutlineEntry>, chapterIndex: Int): Int {
    val target = rows.firstOrNull { it.index == chapterIndex }
        ?: rows.filter { it.index < chapterIndex }.maxByOrNull { it.index }
    return rows.indexOf(target).coerceAtLeast(0)
}

internal fun catalogPageIndices(rows: List<CatalogOutlineEntry>, offset: Int, limit: Int): List<Int> {
    val start = offset.coerceIn(0, rows.size)
    val end = (start + limit.coerceAtLeast(0)).coerceAtMost(rows.size)
    return rows.subList(start, end).map { it.index }
}
