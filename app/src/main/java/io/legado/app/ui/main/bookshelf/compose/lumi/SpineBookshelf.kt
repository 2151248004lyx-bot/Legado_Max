package io.legado.app.ui.main.bookshelf.compose.lumi

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.ui.main.bookshelf.compose.BookshelfBookItem
import io.legado.app.ui.theme.AppDimens
import io.legado.app.ui.widget.components.AppBookCover

private val BoardColor = Color(0xFF8A6E52)
private val BoardShadow = Color(0x33000000)
private val SpineGap = 3.dp
private val OnCoverInk = Color.White
private const val MIN_TITLE = 7.5f

/**
 * 书脊木架书架（外观移植自 Lumi_Books）。
 * 书脊取封面中部竖条填充，之上叠竖排书名 + 底部作者；厚薄 / 高矮 / 倾斜按书名哈希固定并自然错落，
 * 每行按屏宽自动排满。点击 / 长按沿用旧书架。
 */
@Composable
internal fun SpineBookshelf(
    shelfItems: List<BookshelfBookItem>,
    bottomPaddingPx: Int,
    marginPx: Int,
    onScrollBackwardChange: (Boolean) -> Unit,
    onBookClick: (BookshelfBookItem) -> Unit,
    onBookLongClick: (BookshelfBookItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val canScrollBack by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    LaunchedEffect(canScrollBack) { onScrollBackwardChange(canScrollBack) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val rowHeight = maxHeight * 0.26f
        val horizontalPad = AppDimens.shelfContentHorizontalPadding * 2
        val availWidth = (maxWidth - horizontalPad).coerceAtLeast(100.dp)
        // 按每本书脊厚度累加，排满一行再换行，末行不再留大块空当
        val rows = remember(shelfItems, availWidth) {
            val gap = SpineGap.value
            val result = mutableListOf<MutableList<BookshelfBookItem>>()
            var cur = mutableListOf<BookshelfBookItem>()
            var used = 0f
            for (item in shelfItems) {
                val t = SpineTraitsProvider.traits(item.key).thickness
                if (cur.isNotEmpty() && used + gap + t > availWidth.value) {
                    result.add(cur)
                    cur = mutableListOf()
                    used = 0f
                }
                cur.add(item)
                used += if (cur.size == 1) t else gap + t
            }
            if (cur.isNotEmpty()) result.add(cur)
            result
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppDimens.shelfContentHorizontalPadding,
                top = marginPx.dp,
                end = AppDimens.shelfContentHorizontalPadding,
                bottom = (marginPx + bottomPaddingPx).dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(rows, key = { row -> row.firstOrNull()?.key ?: row.hashCode() }) { row ->
                SpineRow(
                    books = row,
                    rowHeight = rowHeight,
                    onBookClick = onBookClick,
                    onBookLongClick = onBookLongClick,
                )
            }
        }
    }
}

@Composable
private fun SpineRow(
    books: List<BookshelfBookItem>,
    rowHeight: Dp,
    onBookClick: (BookshelfBookItem) -> Unit,
    onBookLongClick: (BookshelfBookItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(SpineGap),
        ) {
            books.forEach { bookItem ->
                SpineFace(
                    bookItem = bookItem,
                    rowHeight = rowHeight,
                    onClick = { onBookClick(bookItem) },
                    onLongClick = { onBookLongClick(bookItem) },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BoardColor),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(BoardShadow),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpineFace(
    bookItem: BookshelfBookItem,
    rowHeight: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val display = bookItem.display
    val density = LocalDensity.current
    val traits = remember(bookItem.key) { SpineTraitsProvider.traits(bookItem.key) }
    val spineW = traits.thickness.dp
    val spineH = rowHeight * traits.heightFraction
    val fallbackBg = Color(SpineTraitsProvider.palette[traits.colorIndex])

    val title = SpineText.mainTitle(display.name)
    val author = SpineText.shortAuthor(display.author ?: "")
    val units = remember(title) { SpineText.verticalUnits(title) }
    val baseSize = (traits.thickness * 0.42f).coerceIn(8.5f, 14f)
    val availPx = with(density) { (spineH - 12.dp).toPx() }
    fun lineHeightPx(sizeSp: Float) = with(density) { sizeSp.sp.toPx() } * 1.15f
    var titleSize = baseSize
    var capacity = (availPx / lineHeightPx(baseSize)).toInt().coerceAtLeast(1)
    if (units.size > capacity) {
        titleSize = (availPx / (units.size * 1.15f)).coerceIn(MIN_TITLE, baseSize)
        capacity = units.size
    }
    val shownUnits = units.take(capacity)

    Box(
        modifier = Modifier
            .width(spineW)
            .height(spineH)
            .graphicsLayer {
                rotationZ = -traits.leanDegrees
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(RoundedCornerShape(2.dp))
            .background(fallbackBg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AppBookCover(
            modifier = Modifier.fillMaxSize(),
            name = display.name,
            author = display.author,
            coverPath = display.getDisplayCover(),
            galleryIdentity = display.bookUrl,
            sourceOrigin = display.origin,
            contentDescription = display.name,
            cornerRadius = 2.dp,
        )
        // 薄暗化保证叠字可读
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                shownUnits.forEach { ch ->
                    Text(
                        text = ch,
                        color = OnCoverInk,
                        fontSize = titleSize.sp,
                        lineHeight = (titleSize * 1.15f).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
            if (author.isNotBlank()) {
                Text(
                    text = author,
                    color = OnCoverInk.copy(alpha = 0.78f),
                    fontSize = 7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
