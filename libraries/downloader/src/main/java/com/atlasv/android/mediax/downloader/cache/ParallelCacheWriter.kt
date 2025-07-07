package com.atlasv.android.mediax.downloader.cache

import android.util.Log
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import com.atlasv.android.logger.ILogger
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.core.MediaXCache
import com.atlasv.android.mediax.downloader.datasource.getCachedBytes
import com.atlasv.android.mediax.downloader.datasource.getContentLength
import com.atlasv.android.mediax.downloader.datasource.removeResourceWithTrack
import com.atlasv.android.mediax.downloader.datasource.saveDataSpecOrThrow
import com.atlasv.android.mediax.downloader.exception.isIoCancelException
import com.atlasv.android.mediax.downloader.exception.wrapAsDownloadFailedException
import com.atlasv.android.mediax.downloader.output.DownloadResult
import com.atlasv.android.mediax.downloader.output.OutputTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.InterruptedIOException
import kotlin.concurrent.Volatile

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
    private val downloadListener: DownloadListener?,
    private val retryTimes: Int = 3,
    private val logger: ILogger? = null
) {
    private val parallelProgressListener =
        ParallelProgressListener(uriString = uriString, taskId = taskId, downloadListener)
    private val cacheWriters = mutableSetOf<CacheWriter>()
    private var jobs: List<Deferred<Unit?>>? = null
    @Volatile
    private var isCanceled = false
    // 是否标记为删除
    private var needDelete: Boolean = false

    fun getProgress(): Float {
        return parallelProgressListener.getProgress()
    }

    suspend fun cache(): DownloadResult {
        return coroutineScope {
            val dataSpecs = createDataSpecs()
            val alreadyCacheBytes = mediaXCache.cache.getCachedBytes(taskId)
            val isNewTask = alreadyCacheBytes <= 0
            val rangeCount = dataSpecs.size
            jobs = dataSpecs.mapIndexed { index, dataSpec ->
                async {
                    val cacheWriter = createRealCacheWriter(index, rangeCount, dataSpec)
                    try {
                        cacheWriters.add(cacheWriter)
                        cacheWithRetry(cacheWriter)
                    } catch (cause: InterruptedIOException) {
                        logger?.w(cause) { "ParallelCacheWriter catch InterruptedIOException($uriString)" }
                        throw CancellationException(
                            "ParallelCacheWriter canceled by InterruptedIOException",
                            cause
                        )
                    } catch (cause: Throwable) {
                        logger?.e(cause) { "ParallelCacheWriter catch ${cause.javaClass.simpleName}($uriString)" }
                        throw cause
                    }
                }
            }
            try {
                if (isNewTask) {
                    downloadListener?.onDownloadStart(taskId, uriString)
                } else {
                    downloadListener?.onDownloadRestart(taskId, uriString)
                }
                jobs?.awaitAll()
                logger?.d { "[${Thread.currentThread().name}]onDownloadSuccess: uriString=$uriString, taskId=$taskId" }
                downloadListener?.onDownloadSuccess(taskId, uriString, rangeCount)
                val fileLength = saveToOutputStream(uriString, outputTarget)
                logger?.d { "[${Thread.currentThread().name}]onSaveSuccess($fileLength): uriString=$uriString, taskId=$taskId" }
                downloadListener?.onSaveSuccess(taskId, uriString, fileLength, outputTarget)
                DownloadResult(taskId = taskId, uriString, outputTarget, fileLength)
            } catch (cause: CancellationException) {
                if (needDelete) {
                    deleteResource(uriString)
                } else {
                    logger?.d { "ParallelCacheWriter all jobs are canceled($uriString)" }
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

    private fun cacheWithRetry(cacheWriter: CacheWriter) {
        // 最多执行retryTimes+1次，重试3次则最多执行4次
        for (runTimes in 0..retryTimes) {
            try {
                Log.d("MediaXDownloaderCore", " start write cache : $cacheWriter canceled: $isCanceled")
                logger?.d { "[${Thread.currentThread().name}]cacheWithRetry($runTimes/$retryTimes) start..." }
                cacheWriter.cache()
                logger?.d { "[${Thread.currentThread().name}]cacheWithRetry($runTimes/$retryTimes) success" }
                break
            } catch (cause: Throwable) {
                // 主动中断的情况不需要重试
                if (cause !is InterruptedIOException && runTimes < retryTimes) {
                    logger?.e(cause) { "cacheWithRetry($runTimes/$retryTimes) failed, will retry" }
                } else {
                    throw cause
                }
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
        ).apply {
            if (isCanceled){
                cancel()
            }
        }
    }

    private fun deleteResource(uriString: String) {
        logger?.w { "[${Thread.currentThread().name}]ParallelCacheWriter jobs are deleted, will remove resource($uriString)" }
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
        dataSource.saveDataSpecOrThrow(dataSpec, outputTarget)
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
        val rangeLength =
            if (isContentLengthKnown) estimateContentLength / rangeCount else estimateContentLength
        logger?.d { "[${Thread.currentThread().name}]Set range count to $rangeCount, estimateContentLength=$estimateContentLength, uriString=$uriString" }
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
            logger?.d { "[${Thread.currentThread().name}]Build DataSpec: $dataSpec" }
            dataSpec
        }
    }

    fun cancel(alsoDelete: Boolean = false) {
        needDelete = alsoDelete
        isCanceled = true
        try {
            cacheWriters.forEach {
                it.cancel()
            }
        } catch (cause: Throwable) {
            logger?.e(cause) { "ParallelCacheWriter cancel exception occurred($uriString)" }
        }
    }
}

