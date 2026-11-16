package io.legado.app.ui.book.read

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.script.rhino.runScriptWithContext
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.source.getSourceType
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.ui.login.SourceLoginJsExtensions
import io.legado.app.ui.widget.NgActionPopupItem
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.isTrue
import io.legado.app.utils.startActivity
import kotlinx.coroutines.Dispatchers.IO

internal fun readSourceActionItems(
    showLogin: Boolean,
    sourceEnabled: Boolean,
    showChapterPay: Boolean,
): List<NgActionPopupItem> = buildList {
    if (showLogin) {
        add(NgActionPopupItem(R.id.menu_login, R.string.login, R.drawable.ic_lock_outline))
    }
    add(NgActionPopupItem(R.id.menu_edit_source, R.string.edit_book_source, R.drawable.ic_edit))
    add(
        NgActionPopupItem(
            itemId = if (sourceEnabled) R.id.menu_disable_source else R.id.menu_enable,
            titleRes = if (sourceEnabled) R.string.disable_book_source else R.string.enable,
            iconRes = if (sourceEnabled) {
                R.drawable.ic_block_outline
            } else {
                R.drawable.ic_check_circle_outline
            }
        )
    )
    if (showChapterPay) {
        add(NgActionPopupItem(R.id.menu_chapter_pay, R.string.chapter_pay, R.drawable.ic_check))
    }
}

internal fun executeReadSourcePayAction(
    activity: AppCompatActivity,
    book: Book,
    chapter: BookChapter,
    bookType: Int,
    sourceProvider: () -> BookSource?,
    onPurchased: () -> Unit,
) {
    Coroutine.async(activity.lifecycleScope) {
        val source = sourceProvider() ?: throw NoStackTraceException("no book source")
        val payAction = source.getContentRule().payAction
        if (payAction.isNullOrBlank()) {
            throw NoStackTraceException("no pay action")
        }
        val java = SourceLoginJsExtensions(activity, source, bookType)
        runScriptWithContext {
            source.evalJS(payAction) {
                put("java", java)
                put("book", book)
                put("chapter", chapter)
                put("title", chapter.title)
                put("baseUrl", chapter.url)
                put("result", null)
                put("src", null)
            }.toString()
        }
    }.onSuccess(IO) {
        if (it.isAbsUrl()) {
            activity.startActivity<WebViewActivity> {
                val bookSource = sourceProvider()
                putExtra("title", activity.getString(R.string.chapter_pay))
                putExtra("url", it)
                putExtra("sourceOrigin", bookSource?.bookSourceUrl)
                putExtra("sourceName", bookSource?.bookSourceName)
                putExtra("sourceType", bookSource?.getSourceType())
            }
        } else if (it.isTrue()) {
            onPurchased()
        }
    }.onError {
        AppLog.put("执行购买操作出错\n${it.localizedMessage}", it, true)
    }
}
