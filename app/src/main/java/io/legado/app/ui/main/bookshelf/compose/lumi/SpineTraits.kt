package io.legado.app.ui.main.bookshelf.compose.lumi

import kotlin.math.abs
import kotlin.random.Random

/**
 * 书脊外观特质（移植自 Lumi SpineLayout 的姿态思路，第一版简化）。
 * 同一本书在一次进程内姿态固定，避免滚动时书脊来回跳变。
 */
internal data class SpineTraits(
    /** 书脊厚度 dp */
    val thickness: Float,
    /** 相对最高书的高度系数 0.80–1.00 */
    val heightFraction: Float,
    /** 倾斜角度，直立为 0 */
    val leanDegrees: Float,
    /** 低饱和底色 */
    val colorIndex: Int,
)

internal object SpineTraitsProvider {
    private const val MIX = -7046029254386353131L

    /** 低饱和莫兰迪书脊色（第一版用预设色，不做封面解码取色） */
    val palette = listOf(
        0xFFE7D8C9.toInt(),
        0xFFD9C7B8.toInt(),
        0xFFCDB7A0.toInt(),
        0xFFC9B8A3.toInt(),
        0xFFB7C4B1.toInt(),
        0xFFAEC3B8.toInt(),
        0xFFB9C6D6.toInt(),
        0xFFC6C2D8.toInt(),
        0xFFD6B9C6.toInt(),
        0xFFE0B7A8.toInt(),
        0xFFD8D2C2.toInt(),
        0xFFBFBFB7.toInt(),
    )

    fun traits(id: String, seed: Long = 0L): SpineTraits {
        val random = Random(seed xor (id.hashCode().toLong() * MIX))
        val thickness = 20f + random.nextFloat() * random.nextFloat() * 20f
        val heightFraction = 0.80f + random.nextFloat() * 0.20f
        val roll = random.nextFloat()
        val lean = if (roll < 0.80f) 0f else (7f + random.nextFloat() * 6f)
        val colorIndex = abs(random.nextInt()) % palette.size
        return SpineTraits(thickness, heightFraction, lean, colorIndex)
    }
}
