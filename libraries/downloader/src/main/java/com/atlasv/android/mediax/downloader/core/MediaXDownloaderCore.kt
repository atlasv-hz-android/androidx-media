package com.atlasv.android.mediax.downloader.core

import androidx.media3.common.C
import com.atlasv.android.mediax.downloader.cache.ParallelCacheWriter
import com.atlasv.android.mediax.downloader.cache.RangeCountStrategy
import com.atlasv.android.mediax.downloader.cache.SimpleRangeStrategy
import com.atlasv.android.mediax.downloader.cache.isSingleRange
import com.atlasv.android.mediax.downloader.datasource.isCacheComplete
import com.atlasv.android.mediax.downloader.datasource.removeResourceWithTrack
import com.atlasv.android.mediax.downloader.output.DownloadResult
import com.atlasv.android.mediax.downloader.output.OutputTarget
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by weiping on 2024/9/7
 */
class MediaXDownloaderCore(
    mediaXCacheSupplier: MediaXCacheSupplier,
    private val contentLengthLoader: ContentLengthLoader,
    private val defaultRangeCountStrategy: RangeCountStrategy = SimpleRangeStrategy.SingleRangeStrategy
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
        outputTarget: OutputTarget,
        rangeCountStrategy: RangeCountStrategy? = null,
        downloadListener: DownloadListener?
    ): DownloadResult? {
        if (writerMap[downloadUrl] != null) {
            throw IllegalStateException("Duplicate task of $downloadUrl")
        }
        val targetRangeCountStrategy = rangeCountStrategy ?: defaultRangeCountStrategy
        val contentLength =
            if (targetRangeCountStrategy.isSingleRange()) C.LENGTH_UNSET.toLong() else contentLengthLoader.getContentLengthOrUnset(
                uriString = downloadUrl
            )
        val cacheWriter =
            createCacheWriter(
                downloadUrl,
                id,
                outputTarget = outputTarget,
                targetRangeCountStrategy,
                downloadListener,
                contentLength
            )
        return try {
            cacheWriter.cache()
            DownloadResult(downloadUrl, outputTarget, contentLength)
        } finally {
            writerMap.remove(downloadUrl)
        }
    }

    private fun createCacheWriter(
        downloadUrl: String,
        id: String,
        outputTarget: OutputTarget,
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
            outputTarget = outputTarget,
            downloadListener = downloadListener
        )
        writerMap[downloadUrl] = writer
        return writer
    }

    fun cancel(uriString: String, alsoDelete: Boolean = false) {
        val writer = writerMap[uriString]
        if (writer != null) {
            writer.cancel(alsoDelete = alsoDelete)
        } else if (alsoDelete) {
            // 暂停状态用户删除下载任务，需要走这个逻辑
            mediaXCache.cache.removeResourceWithTrack(uriString)
        }
    }
}