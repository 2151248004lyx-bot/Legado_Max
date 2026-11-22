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
 * 点开一本书的开书过渡：封面由小放大 → 沿书脊中缝翻开露出内页 → 封面淡出、内页展开接阅读页。
 *
 * 性能要点：动画进度只在各图层的 graphicsLayer 内读取，Composable 只组合一次，
 * 封面图片不会被每一帧重建；属性用平滑曲线连续映射，避免分段硬切。
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
    val density = LocalDensity.current
    LaunchedEffect(bookItem.key) {
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        )
        onFinished()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // 背景暗罩：只动图层透明度，不重组内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = maskAlpha(progress.value) }
                .background(Color.Black),
        )

        // 对开内书页：封面翻走后露出，静态组合一次
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress.value
                    alpha = pageAlpha(p)
                    val s = pageScale(p)
                    scaleX = s
                    scaleY = s
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
            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFC7BCA1)),
            )
        }

        // 封面：沿左中缝翻开，图层内读取动画值
        AppBookCover(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / AppDimens.BOOK_COVER_ASPECT)
                .graphicsLayer {
                    val p = progress.value
                    cameraDistance = with(density) { 18.dp.toPx() }
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    val z = zoomScale(p)
                    scaleX = z
                    scaleY = z
                    rotationY = flipDegrees(p)
                    alpha = coverAlpha(p)
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

/** 把进度限制在 [a,b] 区间并归一化到 0..1 */
private fun ramp(p: Float, a: Float, b: Float): Float =
    ((p - a) / (b - a)).coerceIn(0f, 1f)

/** smoothstep：起步、收尾都柔和，避免匀速硬切 */
private fun ease(t: Float): Float = t * t * (3f - 2f * t)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun zoomScale(p: Float): Float = lerp(0.78f, 1.0f, ease(ramp(p, 0f, 0.6f)))

private fun flipDegrees(p: Float): Float = -170f * ease(ramp(p, 0.25f, 0.9f))

private fun coverAlpha(p: Float): Float = 1f - ramp(p, 0.78f, 1f)

private fun pageAlpha(p: Float): Float = ease(ramp(p, 0.3f, 0.85f))

private fun pageScale(p: Float): Float = lerp(0.96f, 1f, ramp(p, 0.3f, 0.85f))

private fun maskAlpha(p: Float): Float = lerp(0.25f, 0.85f, ramp(p, 0f, 1f))
