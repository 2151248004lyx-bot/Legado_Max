package io.legado.app.ui.main.bookshelf.compose.lumi

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign
import kotlin.math.tanh

/**
 * CoverFlow 几何（移植自 Lumi_Books）。所有距离以像素计，透视投影绘制与命中共用同一套参数。
 * [distance] 为相对正中间一本的索引级偏移：0 居中，±1 紧邻，±2 隔一本……
 */
internal data class CoverFlowGeometry(val coverWidth: Float, val coverHeight: Float) {
    val centerGap = coverWidth * 0.62f
    val stackDistance = coverWidth * 0.12f
    val camera = coverWidth * 6f

    fun transform(distance: Float): CoverFlowTransform {
        val a = abs(distance)
        // tanh 在中间快速展开，两侧再压缩成叠放；过零点平滑，不依赖索引硬切换。
        val focus = 1f - exp(-a * a * 2.5f)
        val side = (a - 1f).coerceAtLeast(0f)
        val rotation = -sign(distance) * (50f * tanh(a * 1.5f) / tanh(1.5f) +
            7f * (1f - exp(-side * side))).coerceAtMost(62f)
        val depth = coverWidth * (0.383f * focus / (1f - exp(-2.5f)) + 0.35f * side * side / (1f + side))
        val projection = camera / (camera + depth)
        val projectedX = centerGap * tanh(distance * 1.6f) / tanh(1.6f) +
            stackDistance * distance * (1f - exp(-a * a)) * 0.8f -
            stackDistance * tanh(distance * 1.6f) / tanh(1.6f) * (1f - exp(-1f)) * 0.8f
        val edgeFade = (4.5f - a).coerceIn(0f, 1f)
        return CoverFlowTransform(
            x = projectedX,
            depth = depth,
            rotationY = rotation,
            scale = projection,
            alpha = (1f - 0.055f * a).coerceAtLeast(0.72f) * edgeFade,
            blurDp = (0.8f * focus / (1f - exp(-2.5f)) + 0.65f * side).coerceAtMost(3f),
            // Android RenderNode 相机按 Skia 72 单位/英寸；直接传像素焦距会把梯形压平。
            cameraDistance = (camera + depth) / AndroidCameraUnit,
        )
    }

    companion object {
        private const val AndroidCameraUnit = 72f
    }
}

internal data class CoverFlowTransform(
    val x: Float,
    val depth: Float,
    val rotationY: Float,
    val scale: Float,
    val alpha: Float,
    val blurDp: Float,
    val cameraDistance: Float,
)

internal object CoverFlowPhysics {
    const val VisibilityRadius = 5

    fun visibleRange(position: Int, count: Int): IntRange {
        if (count == 0) return IntRange.EMPTY
        val center = position.coerceIn(0, count - 1)
        return (center - VisibilityRadius).coerceAtLeast(0)..
            (center + VisibilityRadius).coerceAtMost(count - 1)
    }
}
