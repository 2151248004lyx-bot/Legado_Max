package io.legado.app.ui.main.bookshelf.compose.lumi

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.legado.app.ui.main.bookshelf.compose.BookshelfBookItem
import io.legado.app.ui.theme.AppDimens
import io.legado.app.ui.widget.components.AppBookCover
import io.legado.app.ui.widget.components.AppDrawablePainter

/**
 * 点开一本书的翻书过渡：封面先放大居中，再沿左侧书脊中缝把整页封面翻开，
 * 底下渐显内书页，封面翻到背面后淡出，顺势进入阅读页。
 *
 * 性能要点：
 * - 直接复用书架上已加载好的 [coverDrawable]，转场不再二次请求封面；
 * - 内页只保留底色与书脊中缝两层，减少离屏合成负担；
 * - 动画值只在图层内读取，Composable 只组合一次。
 * 动画播完一次后由父级清空 [bookItem] 并跳阅读页。
 */
@Composable
internal fun BookOpenTransition(
    bookItem: BookshelfBookItem?,
    coverDrawable: Drawable? = null,
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
            tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        )
        onFinished()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // 背景暗罩：只动图层透明度
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = maskAlpha(progress.value) }
                .background(Color.Black),
        )

        // 内书页：封面翻开后露出，仅底色 + 书脊中缝
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
                    .align(Alignment.Center)
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFC7BCA1)),
            )
        }

        // 封面：沿左中缝翻开，优先复用已加载的 Drawable
        Box(
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
        ) {
            val drawable = coverDrawable
            if (drawable != null) {
                Image(
                    painter = remember(drawable) { AppDrawablePainter(drawable) },
                    contentDescription = bookItem.display.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AppDimens.bookCoverCornerRadius)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                AppBookCover(
                    modifier = Modifier.fillMaxSize(),
                    name = bookItem.display.name,
                    author = bookItem.display.author,
                    coverPath = bookItem.display.getDisplayCover(),
                    galleryIdentity = bookItem.display.bookUrl,
                    sourceOrigin = bookItem.display.origin,
                    contentDescription = bookItem.display.name,
                )
            }
        }
    }
}

/** 把进度限制在 [a,b] 区间并归一化到 0..1 */
private fun ramp(p: Float, a: Float, b: Float): Float =
    ((p - a) / (b - a)).coerceIn(0f, 1f)

/** smoothstep：起步、收尾都柔和 */
private fun ease(t: Float): Float = t * t * (3f - 2f * t)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun zoomScale(p: Float): Float = lerp(0.9f, 1.0f, ease(ramp(p, 0f, 0.3f)))

/** 沿左中缝向右翻开至背面 */
private fun flipDegrees(p: Float): Float = -160f * ease(ramp(p, 0.28f, 0.82f))

private fun coverAlpha(p: Float): Float = 1f - ramp(p, 0.82f, 1f)

private fun pageAlpha(p: Float): Float = ease(ramp(p, 0.35f, 0.8f))

private fun pageScale(p: Float): Float = lerp(0.96f, 1f, ramp(p, 0.35f, 0.8f))

private fun maskAlpha(p: Float): Float = lerp(0.2f, 0.85f, ramp(p, 0f, 1f))
