package io.legado.app.ui.main.bookshelf.compose.lumi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.legado.app.ui.main.bookshelf.compose.BookshelfBookItem
import io.legado.app.ui.theme.AppDimens
import io.legado.app.ui.widget.components.AppBookCover

/**
 * 点开一本书的舒缓过渡：封面由小放大居中 → 沿书脊中缝翻开一页 → 展开后自然淡出，
 * 顺势进入阅读页。动画播完一次后由父级清空 [bookItem] 并跳阅读页。
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
        progress.animateTo(
            1f,
            tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        )
        onFinished()
    }

    val density = LocalDensity.current
    val p = progress.value

    // 三段：放大居中 → 中缝翻书 → 展开淡入阅读页
    val zoomT = seg(p, 0f, 0.38f)
    val flipT = seg(p, 0.38f, 0.72f)
    val outT = seg(p, 0.72f, 1f)

    val zoomScale = when {
        p < 0.38f -> lerp(0.72f, 1.0f, zoomT)
        p < 0.72f -> 1.0f
        else -> lerp(1.0f, 1.05f, outT)
    }
    // 沿左侧书脊中缝向右翻开，末段回正
    val flipDegrees = when {
        p < 0.38f -> 0f
        p < 0.72f -> lerp(0f, -135f, flipT)
        else -> lerp(-135f, 0f, outT)
    }
    val coverAlpha = if (p < 0.72f) 1f else lerp(1f, 0f, outT)
    val maskAlpha = lerp(0f, 0.45f, seg(p, 0f, 0.38f)).let {
        if (p < 0.72f) it else lerp(it, 0.85f, outT)
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
                    cameraDistance = with(density) { 10.dp.toPx() }
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    scaleX = zoomScale
                    scaleY = zoomScale
                    rotationY = flipDegrees
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
