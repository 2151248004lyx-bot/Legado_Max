package io.legado.app.ui.main.bookshelf.compose.lumi

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.main.bookshelf.compose.BookshelfBookItem
import io.legado.app.ui.widget.components.AppBookCover
import kotlin.math.abs

/**
 * CoverFlow 三维封面流（外观移植自 Lumi_Books）。
 * 居中本封面朝前；两侧封面沿距离做 rotationY 透视、纵深缩放与淡出叠放，不再使用旧版白色书脊。
 * 点击进阅读、长按沿用旧书架；信息条跟随居中本，可置顶 / 置底。
 */
@Composable
internal fun CoverFlowBookshelf(
    shelfItems: List<BookshelfBookItem>,
    bottomPaddingPx: Int,
    marginPx: Int,
    onScrollBackwardChange: (Boolean) -> Unit,
    onBookClick: (BookshelfBookItem) -> Unit,
    onBookLongClick: (BookshelfBookItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val infoBarTop = AppConfig.bookshelf3dInfoBarTop

    val canScrollBack by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    LaunchedEffect(canScrollBack) { onScrollBackwardChange(canScrollBack) }

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportEndOffset / 2
            var closest = 0
            var minDistance = Float.MAX_VALUE
            layoutInfo.visibleItemsInfo.forEach { item ->
                val distance = abs(item.offset + item.size / 2 - viewportCenter)
                if (distance < minDistance) {
                    minDistance = distance.toFloat()
                    closest = item.index
                }
            }
            closest
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val safeMaxH = maxHeight * 0.80f
        // 封面 3:4，宽按屏宽约三分之一，高随宽，再受安全高度约束。
        var coverW = maxWidth * 0.34f
        var coverH = coverW * 4f / 3f
        val minCoverW = 64.dp
        val minCoverH = 120.dp
        coverW = coverW.coerceIn(minCoverW, maxWidth.coerceAtLeast(minCoverW))
        coverH = coverH.coerceIn(minCoverH, safeMaxH.coerceAtLeast(minCoverH))
        val coverWpx = with(density) { coverW.toPx() }
        val spacingDp = 8.dp

        val geometry = remember(coverWpx) {
            CoverFlowGeometry(coverWidth = coverWpx, coverHeight = with(density) { coverH.toPx() })
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (infoBarTop) {
                CurrentBookBar(
                    bookItem = shelfItems.getOrNull(centerIndex),
                    bottomPaddingPx = bottomPaddingPx,
                    marginPx = marginPx,
                    density = density,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacingDp),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                contentPadding = PaddingValues(
                    horizontal = with(density) {
                        val viewportW = listState.layoutInfo.viewportEndOffset.toFloat()
                        ((viewportW - coverWpx) / 2f).coerceAtLeast(0f).toDp()
                    },
                ),
            ) {
                itemsIndexed(items = shelfItems, key = { _, it -> it.key }) { index, bookItem ->
                    val layoutInfo = listState.layoutInfo
                    val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == index }
                    val viewportCenter = layoutInfo.viewportEndOffset / 2f
                    val distance = if (visibleItem != null) {
                        (visibleItem.offset + visibleItem.size / 2 - viewportCenter) / coverWpx
                    } else {
                        (index - centerIndex).toFloat()
                    }
                    val pose = geometry.transform(distance)
                    CoverFlowCover(
                        bookItem = bookItem,
                        coverW = coverW,
                        coverH = coverH,
                        pose = pose,
                        onClick = { onBookClick(bookItem) },
                        onLongClick = { onBookLongClick(bookItem) },
                    )
                }
            }
            if (!infoBarTop) {
                CurrentBookBar(
                    bookItem = shelfItems.getOrNull(centerIndex),
                    bottomPaddingPx = bottomPaddingPx,
                    marginPx = marginPx,
                    density = density,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoverFlowCover(
    bookItem: BookshelfBookItem,
    coverW: androidx.compose.ui.unit.Dp,
    coverH: androidx.compose.ui.unit.Dp,
    pose: CoverFlowTransform,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val display = bookItem.display
    Box(
        modifier = Modifier
            .width(coverW)
            .height(coverH)
            .graphicsLayer {
                translationX = pose.x
                rotationY = pose.rotationY
                scaleX = pose.scale
                scaleY = pose.scale
                alpha = pose.alpha
                cameraDistance = pose.cameraDistance
            }
            .shadow(16.dp, RoundedCornerShape(6.dp), ambientColor = Color.Black.copy(0.35f), spotColor = Color.Black.copy(0.35f))
            .clip(RoundedCornerShape(6.dp))
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
        )
    }
}

@Composable
private fun CurrentBookBar(
    bookItem: BookshelfBookItem?,
    bottomPaddingPx: Int,
    marginPx: Int,
    density: Density,
    modifier: Modifier = Modifier,
) {
    val display = bookItem?.display ?: return
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = with(density) { (marginPx + bottomPaddingPx).toDp() }),
    ) {
        Text(
            text = display.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (!display.author.isNullOrBlank()) {
            Text(
                text = display.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
