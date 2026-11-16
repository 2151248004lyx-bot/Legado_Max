package io.legado.app.ui.book.manga

import io.legado.app.ui.book.manga.entities.BaseMangaPage

/** Content identities, never image offsets, decide whether a completed turn is still valid. */
internal data class MangaPageTurn(val from: String, val to: String, val direction: Int,
    val pages: List<BaseMangaPage>, val jumpRevision: Int)

internal fun mangaItemKey(pages: List<BaseMangaPage>, index: Int): String =
    pages.getOrNull(index)?.let(::mangaPageKey) ?: mangaTailKey(pages)

/** Retained gesture/auto handlers must resolve both identities at invocation time. */
internal fun mangaBeginTurn(pages: List<BaseMangaPage>, position: Int, direction: Int,
    jumpRevision: Int): MangaPageTurn? {
    if (position !in 0..pages.size || direction != -1 && direction != 1) return null
    val target = position + direction
    if (target !in 0..pages.size) return null
    return MangaPageTurn(mangaItemKey(pages, position), mangaItemKey(pages, target),
        direction, pages, jumpRevision)
}

internal fun mangaAnchorIndex(pages: List<BaseMangaPage>, key: String, fallback: Int): Int {
    if (key == mangaTailKey(pages)) return pages.size
    return pages.indexOfFirst { mangaPageKey(it) == key }.takeIf { it >= 0 }
        ?: fallback.coerceIn(0, pages.size)
}

internal fun mangaCanCommit(turn: MangaPageTurn, current: String, pages: List<BaseMangaPage>, jumpRevision: Int): Boolean =
    turn.from == current && turn.pages === pages && turn.jumpRevision == jumpRevision &&
        (turn.to == mangaTailKey(pages) || pages.any { mangaPageKey(it) == turn.to })

internal fun mangaTurnSign(horizontal: Boolean, rtl: Boolean, direction: Int): Int =
    (if (horizontal && rtl) 1 else -1) * direction

internal fun mangaFinishTurn(progress: Float, forwardVelocity: Float, minimumVelocity: Float): Boolean =
    forwardVelocity > -minimumVelocity && (progress >= 0.25f || progress > 0.02f && forwardVelocity >= minimumVelocity)
