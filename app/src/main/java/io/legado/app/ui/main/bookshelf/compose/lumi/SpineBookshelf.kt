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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.legado.app.ui.main.bookshelf.compose.BookshelfBookItem
import io.legado.app.ui.theme.AppDimens
import io.legado.app.ui.widget.components.AppBookCover

/** 每层立书数量（第一版固定切片，后续可按屏宽自适应） */
private const val BOOKS_PER_ROW = 7
private val BoardColor = Color(0xFF8A6E52)
private val BoardShadow = Color(0x33000000)

/**
 * 书脊木架书架（外观移植自 Lumi_Books）。
 * 书籍按层立在木架上：书脊直接取封面中部竖条（裁剪填满），厚薄 / 高矮 / 倾斜由书名哈希固定，
 * 自然错落；无封面时由封面组件回退默认底色与书名。点击 / 长按沿用旧书架。
 * 第一版不接文件夹厚脊、平放叠放、拖拽重排。
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

    val rows = remember(shelfItems) { shelfItems.chunked(BOOKS_PER_ROW) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val rowHeight = maxHeight * 0.26f
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
            horizontalArrangement = Arrangement.spacedBy(3.dp),
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
        // 木架层板 + 投影
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
    val traits = remember(bookItem.key) { SpineTraitsProvider.traits(bookItem.key) }
    val spineW = traits.thickness.dp
    val spineH = rowHeight * traits.heightFraction
    val fallbackBg = Color(SpineTraitsProvider.palette[traits.colorIndex])

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
    }
}
