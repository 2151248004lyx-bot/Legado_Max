package io.legado.app.ui.main.bookshelf.compose.lumi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.legado.app.ui.main.bookshelf.compose.BookshelfBookItem
import io.legado.app.ui.theme.AppDimens
import io.legado.app.ui.widget.components.AppBookCover

/**
 * 点开一本书的转场动画：封面放大上浮 → 沿中缝翻页 → 展开全屏淡出，随后回调进入阅读页。
 * 动画播完一次后由父级清空 [bookItem] 并跳阅读页；返回书架时状态自然重置。
 */
@Composable
internal fun BookOpenTransition(
    bookItem: BookshelfBookItem?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bookItem == null) {
        Box(modifier = modifier)
        return
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(bookItem.key) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 850, easing = LinearEasing))
        onFinished()
    }

    val density = LocalDensity.current
    val p = progress.value

    // 三段：放大上浮 → 中缝翻页 → 展开全屏淡出
    val zoomT = seg(p, 0f, 0.35f)
    val flipT = seg(p, 0.35f, 0.70f)
    val outT = seg(p, 0.70f, 1f)

    val scale = when {
        p < 0.35f -> lerp(0.78f, 1.02f, zoomT)
        p < 0.70f -> 1.02f
        else -> lerp(1.02f, 1.14f, outT)
    }
    val rotationY = if (p < 0.70f) {
        lerp(0f, -160f, flipT)
    } else {
        lerp(-160f, 0f, outT)
    }
    val coverAlpha = if (p < 0.70f) 1f else lerp(1f, 0f, outT)
    val maskAlpha = lerp(0f, 0.55f, seg(p, 0f, 0.35f)).let {
        if (p < 0.70f) it else lerp(it, 0.92f, outT)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = maskAlpha)),
        )
        AppBookCover(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f / AppDimens.BOOK_COVER_ASPECT)
                .graphicsLayer {
                    cameraDistance = with(density) { 8.dp.toPx() }
                    scaleX = scale
                    scaleY = scale
                    rotationY = rotationY
                    alpha = coverAlpha
                },
            name = bookItem.display.name,
            author = bookItem.display.author,
            coverPath = bookItem.display.getDisplayCover(),
            galleryIdentity = bookItem.display.bookUrl,
            sourceOrigin = bookItem.display.origin,
            contentDescription = bookItem.display.name,
        )
    }
}

private fun seg(p: Float, start: Float, end: Float): Float =
    ((p - start) / (end - start)).coerceIn(0f, 1f)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
