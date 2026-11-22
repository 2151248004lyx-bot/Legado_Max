package io.legado.app.ui.main.bookshelf.compose.lumi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
 * 点开一本书的舒缓开书过渡：
 * 1. 封面由小放大到近全屏；
 * 2. 沿左侧书脊中缝把封面整页翻开，底下渐显对开内书页；
 * 3. 封面翻到背面淡出，内页铺满屏幕，顺势进入阅读页。
 * 动画播完一次后由父级清空 [bookItem] 并跳阅读页。
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
            tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        )
        onFinished()
    }

    val density = LocalDensity.current
    val p = progress.value

    // 三段：放大铺满 → 中缝翻书露出内页 → 封面翻走、内页展开接阅读页
    val zoomT = seg(p, 0f, 0.35f)
    val flipT = seg(p, 0.35f, 0.75f)
    val outT = seg(p, 0.75f, 1f)

    val zoomScale = if (p < 0.35f) lerp(0.78f, 1.0f, zoomT) else 1.0f
    // 沿左中缝向右翻开至背面，末段不回正
    val flipDegrees = when {
        p < 0.35f -> 0f
        p < 0.75f -> lerp(0f, -175f, flipT)
        else -> -175f
    }
    val coverAlpha = if (p < 0.75f) 1f else lerp(1f, 0f, outT)
    val pageAlpha = when {
        p < 0.35f -> 0f
        p < 0.75f -> lerp(0f, 1f, flipT)
        else -> 1f
    }
    val pageScale = if (p < 0.75f) lerp(0.95f, 1f, flipT) else 1f
    val maskAlpha = lerp(0f, 0.35f, seg(p, 0f, 0.35f)).let {
        if (p < 0.75f) it else lerp(it, 0.9f, outT)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // 背景暗罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = maskAlpha)),
        )

        // 对开内书页：封面翻走后露出
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = pageAlpha
                    scaleX = pageScale
                    scaleY = pageScale
                },
        ) {
            Box(Modifier.fillMaxSize().background(Color(0xFFF3ECDC)))
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight()
                    .background(Color(0xFFF7F1E4)),
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight()
                    .background(Color(0xFFEDE5D3)),
            )
            // 书脊中缝
            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFC7BCA1)),
            )
        }

        // 封面：沿左中缝翻开
        AppBookCover(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / AppDimens.BOOK_COVER_ASPECT)
                .graphicsLayer {
                    cameraDistance = with(density) { 20.dp.toPx() }
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
