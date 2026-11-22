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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.ui.main.bookshelf.compose.BookshelfBookItem
import io.legado.app.ui.theme.AppDimens

private val BoardColor = Color(0xFF8A6E52)
private val BoardShadow = Color(0x33000000)
private val SpineGap = 3.dp
private val DarkInk = Color(0xFF2B2118)
private val LightInk = Color(0xFFF8F3EA)
private const val MIN_TITLE = 6.5f

/**
 * 书脊木架书架（外观移植自 Lumi_Books）。
 * 纯色书脊不叠封面图，文字为主体；一部分直立竖排，一部分平放横躺（书名横排完整显示）。
 * 点击 / 长按沿用旧书架。
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

        fun itemWidth(item: BookshelfBookItem): Float {
            val t = SpineTraitsProvider.traits(item.key, SpineText.mainTitle(item.display.name))
            return if (t.flat) t.flatWidth else t.thickness
        }

        // 按每本书脊（或横条）宽度累加，排满一行再换行
        val rows = remember(shelfItems, availWidth) {
            val gap = SpineGap.value
            val result = mutableListOf<MutableList<BookshelfBookItem>>()
            var cur = mutableListOf<BookshelfBookItem>()
            var used = 0f
            for (item in shelfItems) {
                val w = itemWidth(item)
                if (cur.isNotEmpty() && used + gap + w > availWidth.value) {
                    result.add(cur)
                    cur = mutableListOf()
                    used = 0f
                }
                cur.add(item)
                used += if (cur.size == 1) w else gap + w
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
    val title = SpineText.mainTitle(display.name)
    val author = SpineText.shortAuthor(display.author ?: "")
    val traits = remember(bookItem.key) { SpineTraitsProvider.traits(bookItem.key, title) }
    val fallbackBg = Color(SpineTraitsProvider.palette[traits.colorIndex])

    // 按底色亮度反色：亮底深字、暗底浅字，浅字再带黑阴影保证可读
    val luminance = 0.2126f * fallbackBg.red + 0.7152f * fallbackBg.green + 0.0722f * fallbackBg.blue
    val ink = if (luminance > 0.62f) DarkInk else LightInk
    val shadow = if (ink == LightInk) Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 2f) else null

    if (traits.flat) {
        FlatFace(
            title = title,
            author = author,
            bg = fallbackBg,
            ink = ink,
            shadow = shadow,
            traits = traits,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    } else {
        UprightFace(
            title = title,
            author = author,
            bg = fallbackBg,
            ink = ink,
            shadow = shadow,
            traits = traits,
            rowHeight = rowHeight,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UprightFace(
    title: String,
    author: String,
    bg: Color,
    ink: Color,
    shadow: Shadow?,
    traits: SpineTraits,
    rowHeight: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val density = LocalDensity.current
    val spineW = traits.thickness.dp
    val spineH = rowHeight * traits.heightFraction
    val units = remember(title) { SpineText.verticalUnits(title) }
    val baseSize = (traits.thickness * 0.42f).coerceIn(9.5f, 16f)
    val availPx = with(density) { (spineH - 8.dp).toPx() }
    fun lineHeightPx(sizeSp: Float) = with(density) { sizeSp.sp.toPx() } * 1.06f
    var titleSize = baseSize
    var capacity = (availPx / lineHeightPx(baseSize)).toInt().coerceAtLeast(1)
    if (units.size > capacity) {
        titleSize = (availPx / (units.size * 1.06f)).coerceIn(MIN_TITLE, baseSize)
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
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
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
                        color = ink,
                        fontSize = titleSize.sp,
                        lineHeight = (titleSize * 1.06f).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        style = TextStyle(shadow = shadow),
                    )
                }
            }
            if (author.isNotBlank()) {
                Text(
                    text = author,
                    color = ink.copy(alpha = 0.78f),
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlatFace(
    title: String,
    author: String,
    bg: Color,
    ink: Color,
    shadow: Shadow?,
    traits: SpineTraits,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(traits.flatWidth.dp)
            .height(traits.thickness.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                color = ink,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = shadow),
                modifier = Modifier.weight(1f),
            )
            if (author.isNotBlank()) {
                Text(
                    text = author,
                    color = ink.copy(alpha = 0.78f),
                    fontSize = 7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
