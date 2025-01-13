package com.atlasv.android.mediax.downloader.core

import androidx.media3.common.C
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import com.atlasv.android.appcontext.AppContextHolder.Companion.appContext
import com.atlasv.android.mediax.downloader.cache.ParallelCacheWriter
import com.atlasv.android.mediax.downloader.cache.RangeCountStrategy
import com.atlasv.android.mediax.downloader.cache.SimpleRangeStrategy
import com.atlasv.android.mediax.downloader.cache.isSingleRange
import com.atlasv.android.mediax.downloader.datasource.isCacheComplete
import com.atlasv.android.mediax.downloader.datasource.removeResourceWithTrack
import com.atlasv.android.mediax.downloader.listener.withParent
import com.atlasv.android.mediax.downloader.output.DownloadResult
import com.atlasv.android.mediax.downloader.output.OutputTarget
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by weiping on 2024/9/7
 */
class MediaXDownloaderCore(
    okHttpClient: OkHttpClient,
    mediaXCacheSupplier: MediaXCacheSupplier,
    private val defaultRangeCountStrategy: RangeCountStrategy = SimpleRangeStrategy.SingleRangeStrategy,
    private val rootDownloadListener: DownloadListener? = null
) {
    val contentLengthLoader by lazy {
        ContentLengthLoader(okHttpClient)
    }

    private val writerMap = ConcurrentHashMap<String, ParallelCacheWriter>()
    val mediaXCache: MediaXCache by lazy {
        mediaXCacheSupplier.get()
    }

    fun isFullyCached(uriString: String): Boolean {
        return mediaXCache.cache.isCacheComplete(uriString)
    }

    /**
     * [throwException] true: 抛出异常，由调用者自行处理
     */
    suspend fun download(
        downloadUrl: String,
        taskId: String,
        outputTarget: OutputTarget,
        rangeCountStrategy: RangeCountStrategy? = null,
        downloadListener: DownloadListener?,
        throwException: Boolean = false
    ): DownloadResult? {
        if (writerMap[taskId] != null) {
            throw IllegalStateException("Duplicate task of $downloadUrl")
        }
        val targetRangeCountStrategy = rangeCountStrategy ?: defaultRangeCountStrategy
        val estimateContentLength =
            if (targetRangeCountStrategy.isSingleRange()) C.LENGTH_UNSET.toLong() else contentLengthLoader.getContentLengthOrUnset(
                uriString = downloadUrl
            )
        val cacheWriter =
            createCacheWriter(
                downloadUrl,
                taskId,
                outputTarget = outputTarget,
                targetRangeCountStrategy,
                downloadListener.withParent(rootDownloadListener),
                estimateContentLength
            )
        return try {
            cacheWriter.cache()
        } catch (cause: Throwable) {
            if (throwException) {
                throw cause
            }
            // cacheWriter.cache()内部已处理完各种异常，此处只需要返回null
            null
        } finally {
            writerMap.remove(taskId)
        }
    }

    private fun createCacheWriter(
        downloadUrl: String,
        id: String,
        outputTarget: OutputTarget,
        rangeCountStrategy: RangeCountStrategy,
        downloadListener: DownloadListener?,
        estimateContentLength: Long
    ): ParallelCacheWriter {
        val writer = ParallelCacheWriter(
            mediaXCache = mediaXCache,
            uriString = downloadUrl,
            taskId = id,
            rangeCountStrategy = rangeCountStrategy,
            estimateContentLength = estimateContentLength,
            outputTarget = outputTarget,
            downloadListener = downloadListener
        )
        writerMap[id] = writer
        return writer
    }

    fun cancel(id: String, alsoDelete: Boolean = false) {
        val writer = writerMap[id]
        if (writer != null) {
            writer.cancel(alsoDelete = alsoDelete)
        } else if (alsoDelete) {
            // 暂停状态用户删除下载任务，需要走这个逻辑
            mediaXCache.cache.removeResourceWithTrack(id)
        }
    }

    companion object {
        val databaseProvider: DatabaseProvider by lazy {
            // Note: This should be a singleton in your app.
            StandaloneDatabaseProvider(appContext)
        }
    }
}