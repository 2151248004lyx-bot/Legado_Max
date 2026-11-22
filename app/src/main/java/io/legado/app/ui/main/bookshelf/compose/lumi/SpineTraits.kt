package io.legado.app.ui.main.bookshelf.compose.lumi

import kotlin.math.abs
import kotlin.random.Random

/**
 * 书脊外观特质（移植自 Lumi SpineLayout 的姿态思路，第一版简化）。
 * 同一本书在一次进程内姿态固定，避免滚动时书脊来回跳变。
 */
internal data class SpineTraits(
    /** true=平放横躺，false=直立竖排 */
    val flat: Boolean,
    /** 直立：书脊宽 dp；平放：横条高 dp */
    val thickness: Float,
    /** 相对最高书的高度系数 0.70–1.00（直立用） */
    val heightFraction: Float,
    /** 倾斜角度，直立斜靠用，平放为 0 */
    val leanDegrees: Float,
    /** 低饱和底色索引 */
    val colorIndex: Int,
    /** 平放横条宽 dp */
    val flatWidth: Float,
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

    fun traits(id: String, title: String = "", seed: Long = 0L): SpineTraits {
        val random = Random(seed xor (id.hashCode().toLong() * MIX))
        val colorIndex = abs(random.nextInt()) % palette.size
        val roll = random.nextFloat()

        // 约 18% 的书平放横躺，其余直立竖排
        val flat = roll < 0.18f

        return if (flat) {
            // 平放：薄横条，宽度随书名长度撑开，便于横排完整显示
            val titleLen = title.trim().length.coerceAtLeast(3)
            val flatWidth = (titleLen * 13f + 46f).coerceIn(96f, 176f)
            SpineTraits(
                flat = true,
                thickness = 30f + random.nextFloat() * 8f,
                heightFraction = 1f,
                leanDegrees = 0f,
                colorIndex = colorIndex,
                flatWidth = flatWidth,
            )
        } else {
            // 直立：宽 36–58dp，高 0.85–1.00，少量斜靠；加宽加高以容纳竖排书名
            val thickness = 36f + random.nextFloat() * 22f
            val heightFraction = 0.85f + random.nextFloat() * 0.15f
            val lean = if (random.nextFloat() < 0.82f) 0f else (7f + random.nextFloat() * 6f)
            SpineTraits(
                flat = false,
                thickness = thickness,
                heightFraction = heightFraction,
                leanDegrees = lean,
                colorIndex = colorIndex,
                flatWidth = 0f,
            )
        }
    }
}
