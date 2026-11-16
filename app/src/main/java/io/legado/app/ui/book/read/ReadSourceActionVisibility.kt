package io.legado.app.ui.book.read

internal data class ReadSourceActionVisibility(
    val showLogin: Boolean,
    val showChapterPay: Boolean,
)

internal fun resolveReadSourceActionVisibility(
    loginUrl: String?,
    loginUi: String?,
    chapterIsVip: Boolean,
    chapterIsPay: Boolean,
): ReadSourceActionVisibility {
    val hasWebLogin = !loginUrl.isNullOrBlank()
    return ReadSourceActionVisibility(
        showLogin = hasWebLogin || !loginUi.isNullOrBlank(),
        showChapterPay = hasWebLogin && chapterIsVip && !chapterIsPay,
    )
}
