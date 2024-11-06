package com.atlasv.android.mediax.downloader.api

import com.atlasv.android.loader.ResourceContentLoader
import com.atlasv.android.loader.fetch.ResourceContentFetcher
import com.atlasv.android.loader.listener.ResourceContentLoadListener
import com.atlasv.android.mediax.downloader.api.request.DownloadRequest
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.MediaXDownloaderCore
import java.io.File

/**
 * Created by weiping on 2024/9/8
 */
class MediaXDownloadClient(
    val downloadCore: MediaXDownloaderCore,
    downloadListener: DownloadListener?,
    listener: ResourceContentLoadListener<DownloadRequest, File>?
) : ResourceContentLoader<DownloadRequest, File>(listener = listener, enableMemCache = false) {
    override val fetchers: List<ResourceContentFetcher<DownloadRequest, File>> = listOf(
        MediaXDownloadImpl(downloadCore, downloadListener)
    )

    fun isFullyCached(uriString: String): Boolean {
        return downloadCore.isFullyCached(uriString)
    }

}