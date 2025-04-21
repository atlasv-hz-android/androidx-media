package androidx.media3.ui.compose.ext.ui.pager

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.cacheVideoSize
import androidx.media3.common.getCachedVideoSize
import androidx.media3.common.isPlayingUri
import androidx.media3.common.isValid
import androidx.media3.common.playIfNot
import androidx.media3.common.zeroVideoSize
import androidx.media3.ui.compose.ext.data.IMediaItemModel
import androidx.media3.ui.compose.ext.lifecycle.ComposableLifecycle
import androidx.media3.ui.compose.ext.ui.surface.PlayerSurface2
import coil.ImageLoader
import coil.compose.AsyncImage
import com.atlasv.android.logger.ILogger
import com.google.common.net.MediaType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Created by weiping on 2024/7/18
 */
@Composable
fun BoxScope.PlayerPager(
    modifier: Modifier,
    initialPage: Int = 0,
    items: List<IMediaItemModel>,
    pagerItemExtraContent: @Composable (BoxScope.(IMediaItemModel, Player, currentPage: Int, pageCount: Int) -> Unit),
    playerFactory: () -> Player,
    onPageSelected: (Int) -> Unit,
    imageLoader: ImageLoader,
    mediaXLogger: ILogger? = null,
    pageExtraContent: @Composable() (BoxScope.(pagerState: PagerState, realPagerCount: Int, player: Player) -> Unit),
) {
    val player = remember {
        playerFactory()
    }

    ComposableLifecycle(onEvent = { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> player.playIfNot()
            Lifecycle.Event.ON_PAUSE -> player.pause()
            Lifecycle.Event.ON_DESTROY -> player.release()
            else -> {}
        }
    })

    if (items.isEmpty()) {
        return
    }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialPage) {
        if (items.size <= 1) items.size else Int.MAX_VALUE
    }
    val realPagerCount = items.size
    VerticalPager(
        state = pagerState, modifier = modifier
    ) { page: Int ->
        val realPageIndex = page % realPagerCount
        val currentPage = pagerState.currentPage % realPagerCount
        val isCurrentPage = realPageIndex == currentPage
        val itemData = items.getOrNull(realPageIndex) ?: return@VerticalPager
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!isCurrentPage) {
                ImageItemView(itemData.getValidPreviewUri(), imageLoader)
            } else {
                VerticalPagerContent(
                    itemData = itemData.asPurePlayerModel(),
                    player = player,
                    imageLoader = imageLoader,
                    onPlayEnded = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                pagerState.currentPage + 1,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    },
                    extraContent = pagerItemExtraContent,
                    mediaXLogger = mediaXLogger
                )
            }
        }
    }
    pageExtraContent(pagerState, realPagerCount, player)
    LaunchedEffect(key1 = pagerState) {
        snapshotFlow { pagerState.currentPage % realPagerCount }.distinctUntilChanged()
            .collect { page ->
                onPageSelected(page)
            }
    }
}

@Composable
private fun BoxScope.VerticalPagerContent(
    itemData: IMediaItemModel,
    player: Player,
    imageLoader: ImageLoader,
    onPlayEnded: () -> Unit,
    extraContent: @Composable (BoxScope.(IMediaItemModel, Player, currentPage: Int, pageCount: Int) -> Unit),
    mediaXLogger: ILogger?
) {
    if (itemData.isGroup()) {
        val pagerState = rememberPagerState {
            itemData.getChildItems().size
        }
        var pageIndex by remember {
            mutableIntStateOf(0)
        }
        ImagePager(pagerState = pagerState, itemData.getChildItems(), imageLoader)
        extraContent(
            itemData.wrapChildAt(pageIndex),
            player,
            pagerState.currentPage,
            pagerState.pageCount
        )
        LaunchedEffect(key1 = pagerState) {
            snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
                pageIndex = page
            }
        }
        LaunchedEffect(Unit) {
            player.clearMediaItems()
        }
    } else {
        VideoItemView(itemData, player, imageLoader, onPlayEnded = {
            if (player.repeatMode == Player.REPEAT_MODE_OFF) {
                onPlayEnded()
            }
        }, mediaXLogger)
        extraContent(itemData, player, 0, 1)
    }
}

@Composable
private fun ImagePager(
    pagerState: PagerState, items: List<IMediaItemModel>, imageLoader: ImageLoader
) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            items.getOrNull(page)?.also { itemData ->
                ImageItemView(uri = itemData.getUri().takeIf { it.isNotEmpty() }
                    ?: itemData.getValidPreviewUri(),
                    imageLoader)
            }
        }
    }
}

@Composable
private fun ImageItemView(uri: String, imageLoader: ImageLoader) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        imageLoader = imageLoader,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun VideoItemView(
    itemData: IMediaItemModel,
    player: Player,
    imageLoader: ImageLoader,
    onPlayEnded: () -> Unit,
    mediaXLogger: ILogger?
) {
    val scope = rememberCoroutineScope()
    val targetUri = itemData.getUri()
    if (!itemData.getMediaType().`is`(MediaType.ANY_VIDEO_TYPE)) {
        player.clearMediaItems()
        ImageItemView(uri = targetUri.takeIf { it.isNotEmpty() } ?: itemData.getValidPreviewUri(),
            imageLoader = imageLoader)
        return
    }
    val videoSize = itemData.getVideoSize().takeIf { it.isValid() } ?: getCachedVideoSize(targetUri)
    ?: zeroVideoSize()

    /**
     * 播放器首帧是否已渲染
     */
    var isSurfaceShowing by remember {
        mutableStateOf(false)
    }

    var isVideoSizeValid by remember {
        mutableStateOf(videoSize.isValid())
    }

    PlayerSurface2(
        player = player, videoSize, onSurfaceVisibleChanged = {
            isSurfaceShowing = it
        }, onVideoSizeValid = { validVideoSize ->
            isVideoSizeValid = true
            cacheVideoSize(targetUri, validVideoSize)
        }, onPlayEnded = onPlayEnded,
        logger = mediaXLogger
    )

    mediaXLogger?.d { "isSurfaceShowing=$isSurfaceShowing, isVideoSizeValid=$isVideoSizeValid" }
    if (!isSurfaceShowing || !isVideoSizeValid) {
        ImageItemView(uri = itemData.getValidPreviewUri(), imageLoader = imageLoader)
    }

    LaunchedEffect(targetUri) {
        scope.launch {
            if (!player.isPlayingUri(targetUri)) {
                player.setMediaItem(MediaItem.Builder().setUri(targetUri).build())
                player.playWhenReady = true
                player.prepare()
            }
        }
    }
}