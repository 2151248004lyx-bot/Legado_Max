package io.legado.app.ui.main.bookshelf.compose.lumi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.legado.app.ui.main.bookshelf.compose.BookshelfBookItem
import io.legado.app.ui.main.bookshelf.compose.BookshelfDisplayConfig
import io.legado.app.ui.main.bookshelf.compose.BookshelfGridItem
import io.legado.app.ui.theme.AppDimens

private val BoardColor = Color(0xFF8A6E52)
private val BoardShadow = Color(0x33000000)
private const val DEFAULT_COLUMNS = 3

/**
 * 木架封面网格：封面朝前立在木质层板上，条目渲染沿用现有网格封面（书名、进度、角标）。
 * 点击先播放 [BookOpenTransition] 放大翻页转场，结束后再进入阅读页。
 */
@Composable
internal fun ShelfGridBookshelf(
    shelfItems: List<BookshelfBookItem>,
    displayConfig: BookshelfDisplayConfig,
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

    val columns = if (displayConfig.bookLayout >= 2) displayConfig.bookLayout else DEFAULT_COLUMNS
    val rows = remember(shelfItems, columns) { shelfItems.chunked(columns) }
    var opening by remember { mutableStateOf<BookshelfBookItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppDimens.shelfContentHorizontalPadding,
                top = marginPx.dp,
                end = AppDimens.shelfContentHorizontalPadding,
                bottom = (marginPx + bottomPaddingPx).dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(rows, key = { row -> row.firstOrNull()?.key ?: row.hashCode() }) { row ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                BookshelfGridItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    bookItem = item,
                                    displayConfig = displayConfig,
                                    onClick = { opening = it },
                                    onLongClick = onBookLongClick,
                                )
                            }
                        }
                        repeat(columns - row.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                    // 木质层板托底 + 下方投影
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
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
        }

        BookOpenTransition(
            bookItem = opening,
            onFinished = {
                opening?.let { onBookClick(it) }
                opening = null
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
