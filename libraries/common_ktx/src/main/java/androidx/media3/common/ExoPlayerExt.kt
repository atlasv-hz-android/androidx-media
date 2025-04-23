package androidx.media3.common

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * Created by weiping on 2024/7/7
 */

private val videoSizeCache = ConcurrentHashMap<String, VideoSize>()

fun Player.togglePlayState() {
    if (isPlaying) {
        pause()
    } else {
        playOrReplay()
    }
}

fun Player.playOrReplay() {
    if (this.isPlayCompleted()) {
        seekTo(0)
    }
    play()
}

fun Player.seekToProgress(progress: Float) {
    this.seekTo((this.duration * progress).roundToLong())
}

fun Player.isPlayCompleted(): Boolean {
    return this.duration > 0 && this.currentPosition >= this.duration
}

fun Player.isPlayingUri(uri: String): Boolean {
    return currentMediaItem?.localConfiguration?.uri?.toString() == uri
}

fun Player.playIfNot() {
    if (!isPlaying) {
        play()
    }
}

fun VideoSize.isValid(): Boolean {
    return width > 0 && height > 0
}

/**
 * 高:宽
 */
fun VideoSize.getHwRatio(): Float {
    return height.toFloat() / width
}

fun VideoSize.getDesc(): String {
    return "${width}x${height}"
}

fun cacheVideoSize(uri: String, videoSize: VideoSize) {
    videoSizeCache[uri] = videoSize
}

fun getCachedVideoSize(uri: String): VideoSize? {
    return videoSizeCache[uri]?.takeIf { it.isValid() }
}

@OptIn(UnstableApi::class)
fun zeroVideoSize(): VideoSize {
    return VideoSize(0, 0)
}

fun Int.mediaItemTransitionReasonDesc(): String {
    return when (this) {
        Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "repeat"
        Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "auto"
        Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "seek"
        Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "playlist_changed"
        else -> "UnknownReason($this)"
    }
}