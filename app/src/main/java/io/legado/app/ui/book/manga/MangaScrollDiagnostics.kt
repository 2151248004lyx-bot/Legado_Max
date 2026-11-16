package io.legado.app.ui.book.manga

import android.os.SystemClock
import android.util.Log
import io.legado.app.BuildConfig
import java.util.concurrent.atomic.AtomicInteger

/** 临时滚动诊断；仅 Debug 且显式开启日志 tag 时采样，不改变阅读状态。 */
internal object MangaScrollDiagnostics {
    const val TAG = "MangaScrollDiag"
    private val instances = AtomicInteger()
    val enabled: Boolean get() = BuildConfig.DEBUG && Log.isLoggable(TAG, Log.DEBUG)
    fun nextInstance(): Int = if (enabled) instances.incrementAndGet() else 0
    inline fun event(name: String, details: () -> String) {
        if (enabled) Log.d(TAG, "t=${SystemClock.uptimeMillis()} event=$name ${details()}")
    }
}
