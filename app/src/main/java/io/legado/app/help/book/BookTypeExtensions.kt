package io.legado.app.help.book

import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book

private const val mediaTypes = BookType.text or BookType.audio or BookType.image or BookType.video

private fun isEditableBookType(type: Int): Boolean =
    type == BookType.text || type == BookType.audio || type == BookType.image || type == BookType.video

/** 只有在类型栏明确选择后保存，才记录单本书的手动类型。 */
fun Book.setBookTypeFromEditor(type: Int, manuallySelected: Boolean) {
    require(isEditableBookType(type))
    val localType = if (isLocal) BookType.local else 0
    if (manuallySelected) config.customBookType = type
    removeType(mediaTypes, BookType.local)
    addType(type or localType)
}

/** 更新书源类型时仍保留本地、更新失败等状态位，手动类型只覆盖媒体类型。 */
fun Book.updateTypeFromSource(type: Int) {
    removeAllBookType()
    addType(type)
    applyCustomBookType()
}

fun Book.applyCustomBookType() {
    val customType = readConfig?.customBookType?.takeIf(::isEditableBookType) ?: return
    removeType(mediaTypes)
    addType(customType)
}
