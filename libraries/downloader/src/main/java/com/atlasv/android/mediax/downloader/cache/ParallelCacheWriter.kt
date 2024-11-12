package com.atlasv.android.mediax.downloader.cache

import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import com.atlasv.android.mediax.downloader.analytics.DownloadPerfTracker
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.MediaXCache
import com.atlasv.android.mediax.downloader.datasource.removeResourceWithTrack
import com.atlasv.android.mediax.downloader.datasource.saveDataSpec
import com.atlasv.android.mediax.downloader.util.MediaXLoggerMgr.mediaXLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.InterruptedIOException

/**
 * Created by weiping on 2024/11/5
 */
class ParallelCacheWriter(
    private val mediaXCache: MediaXCache,
    private val uriString: String,
    id: String,
    private val rangeCountStrategy: RangeCountStrategy,
    private val contentLength: Long,
    private val destFile: File,
    downloadListener: DownloadListener?,
    private val perfTracker: DownloadPerfTracker?
) {
    private val parallelProgressListener =
        ParallelProgressListener(uriString = uriString, id = id, downloadListener, perfTracker)
    private val cacheWriters = mutableSetOf<CacheWriter>()
    private var jobs: List<Deferred<Unit?>>? = null

    // 是否标记为删除
    private var needDelete: Boolean = false

    fun getProgress(): Float {
        return parallelProgressListener.getProgress()
    }

    suspend fun cache() {
        destFile.parentFile?.mkdirs()
        coroutineScope {
            val dataSpecs = createDataSpecs()
            val rangeCount = dataSpecs.size
            jobs = dataSpecs.mapIndexed { index, dataSpec ->
                async {
                    val cacheWriter = createRealCacheWriter(index, rangeCount, dataSpec)
                    try {
                        cacheWriters.add(cacheWriter)
                        cacheWriter.cache()
                    } catch (cause: InterruptedIOException) {
                        mediaXLogger?.w(cause) { "ParallelCacheWriter catch InterruptedIOException" }
                        throw CancellationException(
                            "ParallelCacheWriter canceled by InterruptedIOException",
                            cause
                        )
                    } catch (cause: Throwable) {
                        mediaXLogger?.e(cause) { "ParallelCacheWriter catch ${cause.javaClass.simpleName}" }
                        throw cause
                    }
                }
            }
            try {
                perfTracker?.trackDownloadStart()
                jobs?.awaitAll()
                perfTracker?.trackDownloadSuccess(rangeCount)
                saveToDestFile(uriString, destFile)
                perfTracker?.trackSaveSuccess(destFile.length())
            } catch (cause: CancellationException) {
                if (needDelete) {
                    deleteResource(uriString)
                } else {
                    mediaXLogger?.d { "ParallelCacheWriter all jobs are canceled" }
                }
                val realReason = cause.cause
                if (realReason != null) {
                    perfTracker?.trackDownloadFailed(realReason)
                }
                throw cause
            } catch (cause: Throwable) {
                perfTracker?.trackDownloadFailed(cause)
                throw cause
            }
        }
    }

    private fun createRealCacheWriter(
        index: Int,
        rangeCount: Int,
        dataSpec: DataSpec
    ): CacheWriter {
        val dataSource = mediaXCache.createDataSource()
        return CacheWriter(
            dataSource, dataSpec, null,
            parallelProgressListener.asProgressListener(index, rangeCount, contentLength)
        )
    }

    private fun deleteResource(uriString: String) {
        mediaXLogger?.w { "ParallelCacheWriter jobs are deleted, will remove resource($uriString)" }
        mediaXCache.cache.removeResourceWithTrack(uriString)
    }

    private fun saveToDestFile(uriString: String, destFile: File) {
        val dataSource = mediaXCache.createDataSource()
        val dataSpec = DataSpec.Builder().setUri(uriString).build()
        dataSource.saveDataSpec(dataSpec, destFile)
        mediaXLogger?.d { "Downloaded to $destFile(${destFile.length()}), url=$uriString" }
        val cacheKey = mediaXCache.cacheKeyFactory.buildCacheKey(dataSpec)
        mediaXCache.cache.removeResourceWithTrack(cacheKey)
    }

    private fun createDataSpecs(): List<DataSpec> {
        val isContentLengthKnown = contentLength > 0
        val rangeCount =
            if (!isContentLengthKnown) 1 else rangeCountStrategy.getRangeCount(contentLength = contentLength)
                .coerceAtLeast(1)
        val rangeLength = if (isContentLengthKnown) contentLength / rangeCount else contentLength
        mediaXLogger?.d { "Set range count to $rangeCount, contentLength=$contentLength, uriString=$uriString" }
        return (0 until rangeCount).map { index ->
            val rangeStart = rangeLength * index
            val dataSpec =
                DataSpec.Builder().setUri(uriString).setPosition(rangeStart).apply {
                    if (index != rangeCount - 1) {
                        setLength(rangeLength)
                    }
                }.build()
            mediaXLogger?.d { "Build DataSpec: $dataSpec" }
            dataSpec
        }
    }

    fun cancel(alsoDelete: Boolean = false) {
        needDelete = alsoDelete
        try {
            cacheWriters.forEach {
                it.cancel()
            }
        } catch (cause: Throwable) {
            mediaXLogger?.e(cause) { "ParallelCacheWriter cancel exception occurred" }
        }
    }
}

