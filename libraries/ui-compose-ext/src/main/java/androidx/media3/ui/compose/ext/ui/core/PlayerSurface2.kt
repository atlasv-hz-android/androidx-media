package androidx.media3.ui.compose.ext.ui.core

import android.view.Surface
import androidx.annotation.OptIn
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.getDesc
import androidx.media3.common.getHwRatio
import androidx.media3.common.isValid
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.atlasv.android.logger.ILogger
import kotlin.math.roundToInt

/**
 *
 *
 * Google 是建议用 [AndroidExternalSurface]，但是这个兼容性不好，在有的手机上画面异常（HUAWEI-AL10, Android 10），
 * 故这里用[AndroidEmbeddedExternalSurface]进行渲染
 *
 * Created by weiping on 2024/7/18
 *
 * See
 * [Choosing a surface type](https://developer.android.com/media/media3/ui/playerview#surfacetype)
 * for more information.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerSurface2(
    player: Player,
    inputVideoSize: VideoSize,
    onSurfaceVisibleChanged: (Boolean) -> Unit,
    onVideoSizeValid: (VideoSize) -> Unit,
    onPlayEnded: () -> Unit,
    logger: ILogger?
) {

    var validVideoSize: VideoSize by remember(inputVideoSize) {
        mutableStateOf(inputVideoSize)
    }

    logger?.d { "PlayerSurface rendered, inputVideoSize=${inputVideoSize.getDesc()}, validVideoSize=${validVideoSize.getDesc()}" }

    val listener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            onSurfaceVisibleChanged(true)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            // 清掉 MediaItem 导致的播放失败不参与回调
            if (playbackState == Player.STATE_ENDED && player.currentMediaItem != null) {
                onPlayEnded()
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (!inputVideoSize.isValid() && videoSize.isValid()) {
                validVideoSize = videoSize
                onVideoSizeValid(videoSize)
                logger?.d { "PlayerSurface onVideoSizeChanged: ${inputVideoSize.getDesc()} -> ${validVideoSize.getDesc()}" }
            }
        }
    }
    val onSurfaceCreated: (Surface) -> Unit = { surface ->
        player.addListener(listener)
        player.setVideoSurface(surface)
    }
    val onSurfaceDestroyed: () -> Unit = {
        // 释放当前播放的资源，避免把残留的画面传递给下一页
        if ((player as? ExoPlayer)?.isReleased != true) {
            player.stop()
        }
        player.setVideoSurface(null)
        player.removeListener(listener)
    }
    val onSurfaceInitialized: (AndroidExternalSurfaceScope) -> Unit =
        { androidExternalSurfaceScope ->
            androidExternalSurfaceScope.onSurface { surface, width, height ->
                onSurfaceCreated(surface)
                surface.onDestroyed {
                    onSurfaceDestroyed()
                }
            }
        }
    val modifier: Modifier = if (validVideoSize.isValid()) {
        val surfaceWidth = LocalConfiguration.current.screenWidthDp
        val surfaceHeight = (validVideoSize.getHwRatio() * surfaceWidth).roundToInt()
        Modifier.size(width = surfaceWidth.dp, height = surfaceHeight.dp)
    } else {
        Modifier.fillMaxSize()
    }
    AndroidEmbeddedExternalSurface(
        onInit = onSurfaceInitialized, modifier = modifier
    )
}

