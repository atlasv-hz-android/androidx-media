package com.atlasv.android.mediax.downloader.cache

import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.MediaXCache
import com.atlasv.android.mediax.downloader.datasource.getContentLength
import com.atlasv.android.mediax.downloader.datasource.removeResourceWithTrack
import com.atlasv.android.mediax.downloader.datasource.saveDataSpec
import com.atlasv.android.mediax.downloader.exception.isIoCancelException
import com.atlasv.android.mediax.downloader.exception.wrapAsDownloadFailedException
import com.atlasv.android.mediax.downloader.output.DownloadResult
import com.atlasv.android.mediax.downloader.output.OutputTarget
import com.atlasv.android.mediax.downloader.util.MediaXLoggerMgr.mediaXLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.InterruptedIOException

/**
 * Created by weiping on 2024/11/5
 */
class ParallelCacheWriter(
    private val mediaXCache: MediaXCache,
    private val uriString: String,
    private val taskId: String,
    private val rangeCountStrategy: RangeCountStrategy,
    private val estimateContentLength: Long,
    private val outputTarget: OutputTarget,
    private val downloadListener: DownloadListener?
) {
    private val parallelProgressListener =
        ParallelProgressListener(uriString = uriString, taskId = taskId, downloadListener)
    private val cacheWriters = mutableSetOf<CacheWriter>()
    private var jobs: List<Deferred<Unit?>>? = null

    // 是否标记为删除
    private var needDelete: Boolean = false

    fun getProgress(): Float {
        return parallelProgressListener.getProgress()
    }

    suspend fun cache(): DownloadResult {
        return coroutineScope {
            val dataSpecs = createDataSpecs()
            val rangeCount = dataSpecs.size
            jobs = dataSpecs.mapIndexed { index, dataSpec ->
                async {
                    val cacheWriter = createRealCacheWriter(index, rangeCount, dataSpec)
                    try {
                        cacheWriters.add(cacheWriter)
                        cacheWriter.cache()
                    } catch (cause: InterruptedIOException) {
                        mediaXLogger?.w(cause) { "ParallelCacheWriter catch InterruptedIOException($uriString)" }
                        throw CancellationException(
                            "ParallelCacheWriter canceled by InterruptedIOException",
                            cause
                        )
                    } catch (cause: Throwable) {
                        mediaXLogger?.e(cause) { "ParallelCacheWriter catch ${cause.javaClass.simpleName}($uriString)" }
                        throw cause
                    }
                }
            }
            try {
                downloadListener?.onDownloadStart(taskId, uriString)
                jobs?.awaitAll()
                downloadListener?.onDownloadSuccess(taskId, uriString, rangeCount)
                val fileLength = saveToOutputStream(uriString, outputTarget)
                downloadListener?.onSaveSuccess(taskId, uriString, fileLength, outputTarget)
                DownloadResult(taskId = taskId, uriString, outputTarget, fileLength)
            } catch (cause: CancellationException) {
                if (needDelete) {
                    deleteResource(uriString)
                } else {
                    mediaXLogger?.d { "ParallelCacheWriter all jobs are canceled($uriString)" }
                }
                val realReason = cause.cause
                    ?.takeIf { !it.isIoCancelException() }
                    ?.wrapAsDownloadFailedException(downloadUrl = uriString)
                if (realReason != null) {
                    downloadListener?.onDownloadFailed(taskId, uriString, realReason)
                }
                throw (realReason ?: cause)
            } catch (cause: Throwable) {
                val downloadException = cause.wrapAsDownloadFailedException(downloadUrl = uriString)
                downloadListener?.onDownloadFailed(taskId, uriString, downloadException)
                throw downloadException
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
            parallelProgressListener.asProgressListener(index, rangeCount, estimateContentLength)
        )
    }

    private fun deleteResource(uriString: String) {
        mediaXLogger?.w { "ParallelCacheWriter jobs are deleted, will remove resource($uriString)" }
        mediaXCache.cache.removeResourceWithTrack(uriString)
    }

    private fun createDataSpecBuilder(key: String, uriString: String): DataSpec.Builder {
        return DataSpec.Builder().setKey(key).setUri(uriString)
    }

    private fun saveToOutputStream(
        uriString: String,
        outputTarget: OutputTarget
    ): Long {
        val dataSource = mediaXCache.createDataSource()
        val dataSpec = createDataSpecBuilder(key = taskId, uriString = uriString).build()
        dataSource.saveDataSpec(dataSpec, outputTarget)
        val cacheKey = mediaXCache.cacheKeyFactory.buildCacheKey(dataSpec)
        val contentLength = mediaXCache.cache.getContentLength(cacheKey)
        mediaXCache.cache.removeResourceWithTrack(cacheKey)
        return contentLength
    }

    private fun createDataSpecs(): List<DataSpec> {
        val isContentLengthKnown = estimateContentLength > 0
        val rangeCount =
            if (!isContentLengthKnown) 1 else rangeCountStrategy.getRangeCount(contentLength = estimateContentLength)
                .coerceAtLeast(1)
        val rangeLength = if (isContentLengthKnown) estimateContentLength / rangeCount else estimateContentLength
        mediaXLogger?.d { "Set range count to $rangeCount, estimateContentLength=$estimateContentLength, uriString=$uriString" }
        return (0 until rangeCount).map { index ->
            val rangeStart = rangeLength * index
            val dataSpec =
                createDataSpecBuilder(key = taskId, uriString = uriString)
                    .setPosition(rangeStart)
                    .apply {
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
            mediaXLogger?.e(cause) { "ParallelCacheWriter cancel exception occurred($uriString)" }
        }
    }
}

