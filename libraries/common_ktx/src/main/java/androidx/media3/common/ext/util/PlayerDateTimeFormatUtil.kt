package androidx.media3.common.ext.util

import java.util.Formatter
import java.util.concurrent.TimeUnit

/**
 * Created by weiping on 2024/7/20
 */
object PlayerDateTimeFormatUtil {
    fun formatTime(timeMs: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMs)
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60
        return Formatter().use {
            it.format("%02d:%02d", minutes, seconds).toString()
        }
    }
}