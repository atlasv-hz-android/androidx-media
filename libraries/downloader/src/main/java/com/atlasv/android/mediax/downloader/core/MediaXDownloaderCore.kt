package com.atlasv.android.mediax.downloader.core

import androidx.media3.common.C
import com.atlasv.android.loader.request.ContentRequestStringModel
import com.atlasv.android.mediax.downloader.cache.ParallelCacheWriter
import com.atlasv.android.mediax.downloader.cache.RangeCountStrategy
import com.atlasv.android.mediax.downloader.datasource.isCacheComplete
import java.io.File

/**
 * Created by weiping on 2024/9/7
 */
class MediaXDownloaderCore(
    mediaXCacheSupplier: MediaXCacheSupplier,
    private val contentLengthLoader: ContentLengthLoader
) {
    private val mediaXCache: MediaXCache by lazy {
        mediaXCacheSupplier.get()
    }

    fun isFullyCached(uriString: String): Boolean {
        return mediaXCache.cache.isCacheComplete(uriString)
    }

    suspend fun download(
        downloadUrl: String,
        id: String,
        destFile: File,
        rangeCountStrategy: RangeCountStrategy,
        downloadListener: DownloadListener?
    ): File {
        val contentLength =
            contentLengthLoader.fetch(ContentRequestStringModel(downloadUrl)).result?.takeIf { it > 0 }
                ?: C.LENGTH_UNSET.toLong()
        val cacheWriter = ParallelCacheWriter(mediaXCache)
        cacheWriter.cache(
            uriString = downloadUrl,
            destFile = destFile,
            contentLength = contentLength,
            rangeCountStrategy = rangeCountStrategy,
            progressListener = { requestLength, bytesCached, newBytesCached ->
                downloadListener?.onProgress(
                    requestLength,
                    bytesCached,
                    newBytesCached,
                    downloadUrl,
                    id
                )
            })
        return destFile
    }
}