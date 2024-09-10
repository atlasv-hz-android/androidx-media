package com.atlasv.android.mediax.downloader.core

import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import com.atlasv.android.mediax.downloader.datasource.saveDataSpec
import java.io.File

/**
 * Created by weiping on 2024/9/7
 */
class MediaXDownloaderCore(
    mediaXCacheSupplier: MediaXCacheSupplier
) {
    private val mediaXCache: MediaXCache by lazy {
        mediaXCacheSupplier.get()
    }

    fun download(
        downloadUrl: String, id: String, destFile: File, downloadListener: DownloadListener?
    ): File {
        val dataSource = mediaXCache.createDataSource()
        val dataSpec = DataSpec.Builder().setUri(downloadUrl).build()
        val cacheWriter = CacheWriter(
            dataSource, dataSpec, null
        ) { requestLength, bytesCached, newBytesCached ->
            downloadListener?.onProgress(
                requestLength, bytesCached, newBytesCached, downloadUrl, id
            )
        }
        cacheWriter.cache()
        dataSource.saveDataSpec(dataSpec, destFile)
        val cacheKey = mediaXCache.cacheKeyFactory.buildCacheKey(dataSpec)
        mediaXCache.cache.removeResource(cacheKey)
        return destFile
    }
}