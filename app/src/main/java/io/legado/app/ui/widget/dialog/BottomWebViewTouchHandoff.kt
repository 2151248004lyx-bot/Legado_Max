package io.legado.app.ui.widget.dialog

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.webkit.WebView
import kotlin.math.abs

/** Ordinary WebView does not dispatch nested scrolls merely by enabling the View flag. */
internal class BottomWebViewTouchHandoff(
    private val webView: WebView,
    private val canDragSheet: () -> Boolean,
) : View.OnTouchListener {
    private val touchSlop = ViewConfiguration.get(webView.context).scaledTouchSlop
    private val consumed = IntArray(2)
    private var downX = 0f
    private var downY = 0f
    private var lastY = 0f
    private var draggingSheet = false
    private var blocked = false
    private var consumeUntilUp = false

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (!webView.isNestedScrollingEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                draggingSheet = false
                blocked = false
                consumeUntilUp = false
                downX = event.rawX
                downY = event.rawY
                lastY = event.rawY
                webView.startNestedScroll(View.SCROLL_AXIS_VERTICAL)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Keep pinch zoom and other multi-pointer gestures with WebView.
                blocked = true
                val handled = draggingSheet
                finish()
                consumeUntilUp = handled
                return handled
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = (lastY - event.rawY).toInt()
                lastY = event.rawY
                if (blocked) return consumeUntilUp
                val verticalPull = event.rawY - downY > touchSlop &&
                    abs(event.rawY - downY) > abs(event.rawX - downX)
                if (!draggingSheet && (!verticalPull || dy >= 0 ||
                        webView.canScrollVertically(-1) || !canDragSheet())) {
                    return false
                }
                consumed.fill(0)
                webView.dispatchNestedPreScroll(0, dy, consumed, null)
                if (consumed[1] != 0 && !draggingSheet) {
                    draggingSheet = true
                    // End the WebView gesture once the parent has taken ownership.
                    val cancel = MotionEvent.obtain(event)
                    cancel.action = MotionEvent.ACTION_CANCEL
                    webView.onTouchEvent(cancel)
                    cancel.recycle()
                }
                return draggingSheet
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val handled = draggingSheet || consumeUntilUp
                finish()
                consumeUntilUp = false
                return handled
            }
        }
        return draggingSheet || consumeUntilUp
    }

    fun finish() {
        webView.stopNestedScroll()
        draggingSheet = false
    }
}
