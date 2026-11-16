package io.legado.app.ui.book.read.page

import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import io.legado.app.ui.book.read.page.entities.PageDirection

/** Shared cover/shadow geometry; page content and committing remain with each reader. */
internal class PageCoverRenderer {
    private var viewWidth = 0
    private var viewHeight = 0
    private val shadowDrawableR: GradientDrawable

    init {
        val shadowColors = intArrayOf(0x66111111, 0x00000000)
        shadowDrawableR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, shadowColors
        )
        shadowDrawableR.gradientType = GradientDrawable.LINEAR_GRADIENT
    }

    fun draw(canvas: Canvas, offsetX: Float, direction: PageDirection,
        nextWidth: Float = viewWidth.toFloat(), nextHeight: Float = viewHeight.toFloat(),
        current: (Canvas) -> Unit, adjacent: (Canvas) -> Unit) {
        if ((direction == PageDirection.NEXT && offsetX > 0)
            || (direction == PageDirection.PREV && offsetX < 0)
        ) {
            return
        }
        val distanceX = if (offsetX > 0) offsetX - viewWidth else offsetX + viewWidth
        if (direction == PageDirection.PREV) {
            if (offsetX <= viewWidth) {
                canvas.withTranslation(distanceX) {
                    adjacent(canvas)
                }
                addShadow(distanceX, canvas)
            } else {
                adjacent(canvas)
            }
        } else if (direction == PageDirection.NEXT) {
            val width = nextWidth
            val height = nextHeight
            canvas.withClip(width + offsetX, 0f, width, height) {
                adjacent(this)
            }
            canvas.withTranslation(distanceX - viewWidth) {
                current(this)
            }
            addShadow(distanceX, canvas)
        }
    }

    private fun addShadow(left: Float, canvas: Canvas) {
        if (left == 0f) return
        val dx = if (left < 0) {
            left + viewWidth
        } else {
            left
        }
        canvas.withTranslation(dx) {
            shadowDrawableR.draw(canvas)
        }
    }

    fun setViewSize(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        shadowDrawableR.setBounds(0, 0, 30, viewHeight)
    }
}
