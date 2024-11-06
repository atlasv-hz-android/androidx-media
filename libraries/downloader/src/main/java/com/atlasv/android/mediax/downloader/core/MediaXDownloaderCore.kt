package com.atlasv.android.mediax.downloader.core

import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import com.atlasv.android.loader.request.ContentRequestStringModel
import com.atlasv.android.mediax.downloader.cache.ParallelCacheWriter
import com.atlasv.android.mediax.downloader.cache.RangeCountStrategy
import com.atlasv.android.mediax.downloader.datasource.saveDataSpec
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

    suspend fun download(
        downloadUrl: String,
        id: String,
        destFile: File,
        rangeCountStrategy: RangeCountStrategy,
        downloadListener: DownloadListener?
    ): File {
        val dataSource = mediaXCache.createDataSource()
        val dataSpec = DataSpec.Builder().setUri(downloadUrl).build()
        val temporaryBuffer = ByteArray(CacheWriter.DEFAULT_BUFFER_SIZE_BYTES)
        val contentLength =
            contentLengthLoader.fetch(ContentRequestStringModel(downloadUrl)).result?.takeIf { it > 0 }
                ?: C.LENGTH_UNSET.toLong()
        val cacheWriter = ParallelCacheWriter(mediaXCache)
        cacheWriter.cache(
            uriString = downloadUrl,
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
        dataSource.saveDataSpec(dataSpec, destFile, temporaryBuffer)
        val cacheKey = mediaXCache.cacheKeyFactory.buildCacheKey(dataSpec)
        mediaXCache.cache.removeResource(cacheKey)
        return destFile
    }
}