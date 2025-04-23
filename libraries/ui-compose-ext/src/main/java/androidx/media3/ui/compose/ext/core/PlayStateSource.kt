package androidx.media3.ui.compose.ext.core

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.ext.analytics.PlayerAnalytics
import androidx.media3.common.ext.util.PlayerDateTimeFormatUtil
import androidx.media3.common.mediaItemTransitionReasonDesc
import androidx.media3.common.playIfNot
import androidx.media3.common.togglePlayState
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.compose.ext.preference.PlayerPreferences
import com.atlasv.android.logger.ILogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Created by weiping on 2024/7/20
 */
@SuppressLint("UnsafeOptInUsageError")
class PlayStateSource(
    private val context: Context,
    private val scope: CoroutineScope,
    initRepeatMode: Int,
    private val playerDataSourceFactory: () -> DataSource.Factory,
    private val analyticsHelper: PlayerAnalytics,
    private val mediaXLogger: ILogger?
) : Player.Listener {
    var runningPlayer: Player? = null
    val isPlaying = MutableStateFlow(true)
    private val isLoading = MutableStateFlow(true)
    val playSpeed: MutableStateFlow<Float> = MutableStateFlow(1f)
    val repeatMode = MutableStateFlow(initRepeatMode)
    val showLoading = combine(isPlaying, isLoading) { isPlaying, isLoading ->
        !isPlaying && isLoading
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val currentPosition = MutableStateFlow(0L)
    val duration = MutableStateFlow(0L)
    val shouldShowSwipeGuide =
        PlayerPreferences.shouldShowClickSwipeGuide(appContext = context)
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)
    val playProgress = combine(duration, currentPosition) { duration, currentPosition ->
        if (duration <= 0) {
            0f
        } else {
            currentPosition.toFloat() / duration
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), 0f)
    val playerError = MutableStateFlow<Exception?>(null)
    val currentMediaUri = MutableStateFlow<String?>(null)
    private fun startDispatchProgress() {
        scope.launch {
            while (isActive) {
                val player = runningPlayer
                delay(20)
                if (player == null || player.duration <= 0) {
                    delay(60)
                    continue
                }
                currentPosition.value = player.currentPosition
                duration.value = player.duration
            }
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: @Player.MediaItemTransitionReason Int
    ) {
        val mediaUri = mediaItem?.localConfiguration?.uri?.toString()
        mediaXLogger?.d { "onMediaItemTransition(${mediaUri}): ${reason.mediaItemTransitionReasonDesc()}" }
        currentMediaUri.value = mediaUri
    }

    fun toggleLoopPlay(onNewMode: (Int) -> Unit) {
        repeatMode.update {
            if (it == Player.REPEAT_MODE_ONE) {
                Player.REPEAT_MODE_OFF
            } else {
                Player.REPEAT_MODE_ONE
            }
        }
        runningPlayer?.repeatMode = repeatMode.value
        onNewMode(repeatMode.value)
    }

    fun togglePlayState() {
        runningPlayer?.togglePlayState()
        updatePlayingState()
    }

    fun pausePlayer() {
        runningPlayer?.pause()
        updatePlayingState()
    }

    fun changePlaySpeed(speed: Float) {
        runningPlayer?.setPlaybackSpeed(speed)?.also {
            playSpeed.value = speed
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if (playbackState == Player.STATE_READY) {
            mediaXLogger?.d { "playbackState Ready, uri=${runningPlayer?.currentMediaItem?.localConfiguration?.uri}" }
        }

    }

    val progressText = playProgress.map {
        runningPlayer?.let { player ->
            if (player.duration <= 0) {
                ""
            } else {
                "${PlayerDateTimeFormatUtil.formatTime(player.currentPosition)}/${
                    PlayerDateTimeFormatUtil.formatTime(
                        player.duration
                    )
                }"
            }
        }.orEmpty()
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), "")

    fun createMediaPlayer(): Player {
        return ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(playerDataSourceFactory()))
            /**
             * 预留代码。出现如下错误可尝试开启setRenderersFactory
             * * [dequeueBuffer failed: NO_INIT(-19) error](https://github.com/google/ExoPlayer/issues/10021)
             * * [Video loaded from resources is stuck at BUFFERING state](https://github.com/androidx/media/issues/1132)
             *
             */
//            .setRenderersFactory(
//                DefaultRenderersFactory(context.applicationContext)
//                    .forceDisableMediaCodecAsynchronousQueueing()
//            )
            .build().apply {
                playWhenReady = true
                runningPlayer = this
                repeatMode = this@PlayStateSource.repeatMode.value
                addListener(this@PlayStateSource)
                startDispatchProgress()
                scope.launch {
                    delay(1000)
                    updatePlayingState()
                }
            }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        mediaXLogger?.e(error) { "onPlayerError" }
        playerError.value = error
        analyticsHelper.onPlayerError(error)
    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        super.onPlayerErrorChanged(error)
        mediaXLogger?.e(error) { "onPlayerErrorChanged" }
        playerError.value = error
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        scope.launch {
            if (!isPlaying) {
                delay(3000)
            }
            updatePlayingState()
        }
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        super.onIsLoadingChanged(isLoading)
        scope.launch {
            if (!isLoading) {
                delay(1000)
            }
            this@PlayStateSource.isLoading.value = runningPlayer?.isLoading == true
        }
    }

    private fun updatePlayingState() {
        this@PlayStateSource.isPlaying.value = runningPlayer?.isPlaying == true
    }

    fun playIfNot() {
        runningPlayer?.playIfNot()
        updatePlayingState()
    }

    fun saveHasClickSwipeGuide() {
        scope.launch(Dispatchers.IO) {
            PlayerPreferences.saveHasClickSwipeGuide(appContext = context)
        }
    }
}