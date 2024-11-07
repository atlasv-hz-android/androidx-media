package com.atlasv.android.mediax.downloader.api

import com.atlasv.android.loader.fetch.ResourceContentFetcher
import com.atlasv.android.mediax.downloader.api.request.DownloadRequest
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.MediaXDownloaderCore
import java.io.File

/**
 * Created by weiping on 2024/9/8
 */
class MediaXDownloadImpl(
    private val downloaderCore: MediaXDownloaderCore, private val listener: DownloadListener?
) : ResourceContentFetcher<DownloadRequest, File> {
    override suspend fun fetch(inputData: DownloadRequest): File {
        return downloaderCore.download(
            downloadUrl = inputData.url,
            id = inputData.id,
            destFile = inputData.destFile,
            rangeCountStrategy = inputData.rangeCountStrategy,
            downloadListener = listener
        )
    }

    override fun cancel(key: String) {
    }
}