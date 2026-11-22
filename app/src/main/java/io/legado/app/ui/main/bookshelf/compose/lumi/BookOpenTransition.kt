package io.legado.app.ui.main.bookshelf.compose.lumi

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import io.legado.app.ui.main.bookshelf.compose.BookshelfBookItem
import io.legado.app.ui.theme.AppDimens
import io.legado.app.ui.widget.components.AppBookCover
import io.legado.app.ui.widget.components.AppDrawablePainter

/**
 * 点开一本书的开书过渡：封面由小放大铺满、末段淡出，暗罩渐深后顺势进入阅读页。
 *
 * 性能要点：
 * - 直接复用书架上已加载好的 [coverDrawable]，转场不再二次请求封面；
 * - 不做 3D 透视翻页（避免全屏离屏缓冲掉帧），只走平面缩放与透明度；
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
    LaunchedEffect(bookItem.key) {
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            tween(durationMillis = 750, easing = FastOutSlowInEasing),
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

        // 封面：优先复用已加载的 Drawable；拿不到时回退到完整封面组件
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / AppDimens.BOOK_COVER_ASPECT)
                .graphicsLayer {
                    val p = progress.value
                    val z = zoomScale(p)
                    scaleX = z
                    scaleY = z
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

private fun zoomScale(p: Float): Float = lerp(0.8f, 1.05f, ease(ramp(p, 0f, 0.65f)))

private fun coverAlpha(p: Float): Float = 1f - ramp(p, 0.7f, 1f)

private fun maskAlpha(p: Float): Float = lerp(0.2f, 0.85f, ramp(p, 0f, 1f))
