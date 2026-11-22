package io.legado.app.ui.main.bookshelf.compose.lumi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.legado.app.ui.main.bookshelf.compose.BookshelfBookItem
import io.legado.app.ui.main.bookshelf.compose.BookshelfDisplayConfig
import io.legado.app.ui.main.bookshelf.compose.BookshelfGridItem
import io.legado.app.ui.theme.AppDimens

/** 层板木纹：上沿受光浅棕 → 板身胡桃棕 → 底边最深 */
private val BoardWoodBrush = Brush.verticalGradient(
    0f to Color(0xFFA8875F),
    0.45f to Color(0xFF7C5E3E),
    1f to Color(0xFF4E351F),
)
/** 层板下方落影：上深下透 */
private val BoardShadowBrush = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.38f),
    1f to Color.Black.copy(alpha = 0f),
)
/** 柜格背板：内侧胡桃木，左右略暗，形成柜体纵深 */
private val BackWoodBrush = Brush.horizontalGradient(
    0f to Color(0xFF5A3F27),
    0.5f to Color(0xFF6E5034),
    1f to Color(0xFF523820),
)
/** 两侧挡板木纹，比背板更深，形成柜体夹合感 */
private val SideWoodBrush = Brush.verticalGradient(
    0f to Color(0xFF8A6845),
    1f to Color(0xFF4A321D),
)
private val SidePlateWidth = 11.dp
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(rows, key = { row -> row.firstOrNull()?.key ?: row.hashCode() }) { row ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 柜格：木背板 + 左右侧板，封面立在柜内
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(BackWoodBrush),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // 左侧板
                            Box(
                                modifier = Modifier
                                    .width(SidePlateWidth)
                                    .fillMaxHeight()
                                    .background(SideWoodBrush),
                            )
                            // 书籍区：封面夹在两侧板之间
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                            ) {
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
                            // 右侧板
                            Box(
                                modifier = Modifier
                                    .width(SidePlateWidth)
                                    .fillMaxHeight()
                                    .background(SideWoodBrush),
                            )
                        }
                    }
                    // 木质层板托底 + 下方落影
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(BoardWoodBrush),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(BoardShadowBrush),
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
