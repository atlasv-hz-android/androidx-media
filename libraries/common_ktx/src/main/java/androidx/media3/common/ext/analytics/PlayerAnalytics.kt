package androidx.media3.common.ext.analytics

import androidx.media3.common.PlaybackException

/**
 * Created by weiping on 2024/9/27
 */
interface PlayerAnalytics {
    fun onPlayerError(error: PlaybackException)
}