package com.atlasv.android.mediax.downloader.cache

import androidx.media3.datasource.cache.CacheWriter.ProgressListener
import com.atlasv.android.mediax.downloader.analytics.DownloadPerfTracker
import com.atlasv.android.mediax.downloader.core.CallbackRateLimiter
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.model.SpecProgressInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by weiping on 2024/11/5
 */
class ParallelProgressListener(
    private val uriString: String,
    private val id: String,
    private val downloadListener: DownloadListener?,
    private val perfTracker: DownloadPerfTracker?
) {
    private val specProgressInfoMap = ConcurrentHashMap<Int, SpecProgressInfo>()
    private var currentProgress: Float = 0f
    private var totalNewCachedBytes = 0L
    private var bytesPerSecond = 0L
    private val callbackRateLimiter = CallbackRateLimiter(intervalMillis = 50)
    fun getProgress(): Float {
        return currentProgress
    }

    private fun onProgressAtIndex(
        index: Int,
        rangeCount: Int,
        contentLength: Long,
        requestLength: Long,
        bytesCached: Long,
        newBytesCached: Long
    ) {
        val isLastBytes = requestLength in 1..bytesCached
        totalNewCachedBytes += newBytesCached
        callbackRateLimiter.checkLimit(forceCheck = isLastBytes) { durationMillis: Long ->
            if (durationMillis >= 100) {
                // 计算速度避免极小分母导致极高的峰值
                bytesPerSecond = totalNewCachedBytes * 1000 / durationMillis
            }
            specProgressInfoMap[index] = specProgressInfoMap[index]?.copy(
                bytesCached = bytesCached, requestLength = requestLength
            ) ?: SpecProgressInfo(
                specIndex = index,
                requestLength = requestLength,
                bytesCached = bytesCached
            )
            val (_, mergeBytesCached, mergeContentLength) = calcMergeProgress(
                index,
                contentLength,
                specProgressInfoMap
            )
            currentProgress =
                if (mergeContentLength > 0) mergeBytesCached.toFloat() / mergeContentLength else 0f

            val isAllRangeComplete = mergeContentLength in 1..mergeBytesCached
            if (isAllRangeComplete) {
                perfTracker?.trackDownloadSpeed(uriString, bytesPerSecond, rangeCount)
            }

            downloadListener?.onProgress(
                requestLength = mergeContentLength,
                bytesCached = mergeBytesCached,
                newBytesCached = newBytesCached,
                speedPerSeconds = bytesPerSecond,
                downloadUrl = uriString,
                id = id,
                specProgressInfoMap = specProgressInfoMap
            )
        }
    }

    private fun calcMergeProgress(
        index: Int,
        contentLength: Long,
        infoMap: Map<Int, SpecProgressInfo>
    ): Triple<Int, Long, Long> {
        val values: Collection<SpecProgressInfo> = infoMap.values.toList()
        val validContentLength = contentLength.takeIf { it > 0 } ?: values.sumOf {
            it.requestLength
        }
        val mergeBytesCached = values.sumOf {
            it.bytesCached
        }
        return Triple(index, mergeBytesCached, validContentLength)
    }

    fun asProgressListener(index: Int, rangeCount: Int, contentLength: Long): ProgressListener {
        return ProgressListener { requestLength, bytesCached, newBytesCached ->
            this@ParallelProgressListener.onProgressAtIndex(
                index,
                rangeCount,
                contentLength,
                requestLength,
                bytesCached,
                newBytesCached
            )
        }
    }
}