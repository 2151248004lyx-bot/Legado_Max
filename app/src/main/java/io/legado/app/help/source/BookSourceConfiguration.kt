package io.legado.app.help.source

import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.BookListRule

/**
 * 只判定完整配置是否全空，不把缺少某个入口或联网失败当作无效。
 * 不执行脚本，不调用会创建默认规则对象的 getter，也不修改书源。
 * 新增配置字段时须同步 BookSourceConfigurationTest 的字段覆盖门禁。
 */
fun BookSource.isEmptyConfiguration(): Boolean {
    if (eventListener || customButton || !blank(
            searchUrl, exploreUrl, mainJs, jsLib, loginUrl, loginUi, loginCheckJs,
            bookUrlPattern, coverDecodeJs, header, exploreScreen,
        )
    ) return false
    if (!ruleSearch.emptyListRule() || !ruleSearch?.checkKeyWord.isNullOrBlank()
        || !ruleExplore.emptyListRule()
    ) return false
    ruleBookInfo?.let {
        // canReName、imageStyle 是显示选项，不是抓取配置。
        if (!blank(it.init, it.name, it.author, it.intro, it.kind, it.lastChapter,
                it.updateTime, it.coverUrl, it.tocUrl, it.wordCount, it.downloadUrls)
        ) return false
    }
    ruleToc?.let {
        if (!blank(it.preUpdateJs, it.chapterList, it.chapterName, it.chapterUrl,
                it.formatJs, it.isVolume, it.isVip, it.isPay, it.updateTime, it.nextTocUrl)
        ) return false
    }
    ruleContent?.let {
        if (!blank(it.content, it.subContent, it.title, it.nextContentUrl, it.webJs,
                it.sourceRegex, it.replaceRegex, it.imageDecode, it.payAction, it.callBackJs)
        ) return false
    }
    ruleReview?.let {
        if (!blank(it.reviewUrl, it.avatarRule, it.contentRule, it.postTimeRule,
                it.reviewQuoteUrl, it.voteUpUrl, it.voteDownUrl, it.postReviewUrl,
                it.postQuoteUrl, it.deleteUrl)
        ) return false
    }
    return true
}

private fun BookListRule?.emptyListRule(): Boolean = this == null || blank(
    bookList, name, author, intro, kind, lastChapter, updateTime, bookUrl, coverUrl, wordCount,
)

private fun blank(vararg values: String?): Boolean = values.all { it.isNullOrBlank() }
