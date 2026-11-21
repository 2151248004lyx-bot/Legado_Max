package io.legado.app.ui.main.bookshelf.compose.lumi

/** 书脊文字处理（移植自 Lumi SpineText 的简化版）：截主标题、竖排逐字、作者缩写。 */
internal object SpineText {
    private val leadingGroup = Regex("^\\s*[（(【\\[〔][^）)】\\]〕]*[）)】\\]〕]\\s*")
    private val subtitleMarks = charArrayOf('：', ':', '（', '(', '【', '[', '〔', '—', '–', '|', '｜', '／')

    fun mainTitle(raw: String): String {
        var title = raw.trim().replace(leadingGroup, "").trim()
        title = title.removePrefix("《").removeSuffix("》").trim()
        val dash = title.indexOf(" - ").takeIf { it >= 2 }
        val mark = title.indexOfFirst { it in subtitleMarks }.takeIf { it >= 2 }
        val cut = listOfNotNull(dash, mark).minOrNull()
        if (cut != null) title = title.substring(0, cut)
        title = title.trim().trimEnd('—', '–', '-', '·', ' ', '，', ',')
        return title.ifBlank { raw.trim() }
    }

    fun shortAuthor(raw: String): String {
        val first = raw.split(Regex("\\s*(,|，|、|/|／|&|;|；)\\s*")).firstOrNull { it.isNotBlank() }
            ?: return ""
        return first.replace(" ", "").take(4)
    }

    fun isMostlyCjk(text: String): Boolean {
        if (text.isEmpty()) return false
        val cjk = text.count { it.code in 0x4E00..0x9FFF }
        return cjk * 2 >= text.length
    }

    /** 竖排单元：CJK 逐字；短拉丁/数字成组，长串逐字。 */
    fun verticalUnits(text: String): List<String> {
        val units = ArrayList<String>()
        var index = 0
        while (index < text.length) {
            if (text[index].isAsciiAlphanumeric()) {
                var end = index
                while (end < text.length && text[end].isAsciiAlphanumeric()) end++
                val run = text.substring(index, end)
                if (run.length <= 2) units += run else run.forEach { units += it.toString() }
                index = end
                continue
            }
            val point = text.codePointAt(index)
            val char = String(Character.toChars(point))
            index += Character.charCount(point)
            if (char.isNotBlank()) units += char
        }
        return units
    }

    private fun Char.isAsciiAlphanumeric() =
        this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'
}
