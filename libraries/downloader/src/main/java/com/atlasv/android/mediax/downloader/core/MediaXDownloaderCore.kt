package com.atlasv.android.mediax.downloader.core

import com.atlasv.android.mediax.downloader.cache.ParallelCacheWriter
import com.atlasv.android.mediax.downloader.cache.RangeCountStrategy
import com.atlasv.android.mediax.downloader.datasource.isCacheComplete
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by weiping on 2024/9/7
 */
class MediaXDownloaderCore(
    mediaXCacheSupplier: MediaXCacheSupplier,
    private val contentLengthLoader: ContentLengthLoader
) {
    private val writerMap = ConcurrentHashMap<String, ParallelCacheWriter>()
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
        if (writerMap[downloadUrl] != null) {
            throw IllegalStateException("Duplicate task of $downloadUrl")
        }
        val contentLength = contentLengthLoader.getContentLengthOrUnset(uriString = downloadUrl)
        val cacheWriter =
            createCacheWriter(
                downloadUrl,
                id,
                destFile,
                rangeCountStrategy,
                downloadListener,
                contentLength
            )
        return try {
            cacheWriter.cache()
            destFile
        } finally {
            writerMap.remove(downloadUrl)
        }
    }

    private fun createCacheWriter(
        downloadUrl: String,
        id: String,
        destFile: File,
        rangeCountStrategy: RangeCountStrategy,
        downloadListener: DownloadListener?,
        contentLength: Long
    ): ParallelCacheWriter {
        val writer = ParallelCacheWriter(
            mediaXCache = mediaXCache,
            uriString = downloadUrl,
            id = id,
            rangeCountStrategy = rangeCountStrategy,
            contentLength = contentLength,
            destFile = destFile,
            downloadListener = downloadListener
        )
        writerMap[downloadUrl] = writer
        return writer
    }

    fun cancel(uriString: String, alsoDelete: Boolean = false) {
        writerMap[uriString]?.cancel(alsoDelete = alsoDelete)
    }
}