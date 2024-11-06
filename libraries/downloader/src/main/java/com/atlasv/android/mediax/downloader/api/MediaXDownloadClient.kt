package com.atlasv.android.mediax.downloader.api

import com.atlasv.android.loader.ResourceContentLoader
import com.atlasv.android.loader.fetch.ResourceContentFetcher
import com.atlasv.android.loader.listener.ResourceContentLoadListener
import com.atlasv.android.mediax.downloader.api.request.DownloadRequest
import com.atlasv.android.mediax.downloader.core.ContentLengthLoader
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.MediaXCacheSupplier
import com.atlasv.android.mediax.downloader.core.MediaXDownloaderCore
import java.io.File

/**
 * Created by weiping on 2024/9/8
 */
class MediaXDownloadClient(
    mediaXCacheSupplier: MediaXCacheSupplier,
    val contentLengthLoader: ContentLengthLoader,
    downloadListener: DownloadListener?,
    listener: ResourceContentLoadListener<DownloadRequest, File>?
) : ResourceContentLoader<DownloadRequest, File>(listener = listener, enableMemCache = false) {
    private val downloadCore by lazy {
        MediaXDownloaderCore(mediaXCacheSupplier, contentLengthLoader)
    }

    override val fetchers: List<ResourceContentFetcher<DownloadRequest, File>> = listOf(
        MediaXDownloadImpl(downloadCore, downloadListener)
    )
}