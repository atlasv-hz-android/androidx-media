package com.atlasv.android.mediax.downloader.cache

import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.CacheWriter.ProgressListener
import com.atlasv.android.mediax.downloader.core.MediaXCache
import com.atlasv.android.mediax.downloader.datasource.getCachedBytes
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
    private val mediaXCache: MediaXCache, private val uriString: String,
) {
    private val cacheWriters = mutableSetOf<CacheWriter>()
    private var jobs: List<Deferred<Unit?>>? = null

    // 是否标记为删除
    private var needDelete: Boolean = false
    suspend fun cache(
        destFile: File,
        contentLength: Long,
        rangeCountStrategy: RangeCountStrategy,
        progressListener: ProgressListener?,
    ) {
        destFile.parentFile?.mkdirs()
        val parallelProgressListener = ParallelProgressListener(progressListener)
        coroutineScope {
            val dataSpecs = createDataSpecs(
                uriString,
                contentLength,
                rangeCountStrategy
            )
            jobs = dataSpecs.mapIndexed { index, dataSpec ->
                async {
                    val dataSource = mediaXCache.createDataSource()
                    val cacheWriter = CacheWriter(
                        dataSource, dataSpec, null
                    ) { requestLength, bytesCached, newBytesCached ->
                        parallelProgressListener.onProgress(
                            index,
                            contentLength,
                            requestLength,
                            bytesCached,
                            newBytesCached
                        )
                    }
                    try {
                        cacheWriters.add(cacheWriter)
                        cacheWriter.cache()
                    } catch (cause: InterruptedIOException) {
                        throw CancellationException(
                            "ParallelCacheWriter canceled by InterruptedIOException",
                            cause
                        )
                    }
                }.apply {
                    invokeOnCompletion { cause: Throwable? ->
                        if (cause == null) {
                            mediaXLogger?.d { "ParallelCacheWriter job invokeOnCompletion" }
                        } else {
                            mediaXLogger?.e(cause) { "ParallelCacheWriter job invokeOnCompletion" }
                        }
                    }
                }
            }
            try {
                jobs?.awaitAll()
                saveToDestFile(uriString, destFile)
                mediaXLogger?.d { "Downloaded to $destFile(${destFile.length()}), url=$uriString" }
            } catch (cause: CancellationException) {
                if (needDelete) {
                    mediaXLogger?.w {
                        "ParallelCacheWriter jobs are deleted, will remove resource(${
                            mediaXCache.cache.getCachedBytes(
                                uriString
                            )
                        }), key=$uriString"
                    }
                    mediaXCache.cache.removeResource(uriString)
                    mediaXLogger?.w {
                        "Resource removed,remain size=${
                            mediaXCache.cache.getCachedBytes(
                                uriString
                            )
                        }, key=$uriString"
                    }
                } else {
                    mediaXLogger?.d { "ParallelCacheWriter all jobs are canceled" }
                }
                throw cause
            }
        }
    }

    private fun saveToDestFile(uriString: String, destFile: File) {
        val dataSource = mediaXCache.createDataSource()
        val dataSpec = DataSpec.Builder().setUri(uriString).build()
        dataSource.saveDataSpec(dataSpec, destFile)
        val cacheKey = mediaXCache.cacheKeyFactory.buildCacheKey(dataSpec)
        mediaXCache.cache.removeResource(cacheKey)
    }

    private fun createDataSpecs(
        uriString: String,
        contentLength: Long,
        rangeCountStrategy: RangeCountStrategy
    ): List<DataSpec> {
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

