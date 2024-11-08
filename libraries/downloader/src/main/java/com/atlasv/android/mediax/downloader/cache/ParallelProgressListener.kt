package com.atlasv.android.mediax.downloader.cache

import androidx.media3.datasource.cache.CacheWriter.ProgressListener
import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.model.SpecProgressInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by weiping on 2024/11/5
 */
class ParallelProgressListener(
    private val uriString: String,
    private val id: String,
    private val downloadListener: DownloadListener?
) {
    private val specProgressInfoMap = ConcurrentHashMap<Int, SpecProgressInfo>()
    private var currentProgress: Float = 0f

    fun getProgress(): Float {
        return currentProgress
    }

    private fun onProgressAtIndex(
        index: Int,
        contentLength: Long,
        requestLength: Long,
        bytesCached: Long,
        newBytesCached: Long
    ) {
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
        downloadListener?.onProgress(
            requestLength = mergeContentLength,
            bytesCached = mergeBytesCached,
            newBytesCached = newBytesCached,
            downloadUrl = uriString,
            id = id,
            specProgressInfoMap = specProgressInfoMap
        )
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

    fun asProgressListener(index: Int, contentLength: Long): ProgressListener {
        return ProgressListener { requestLength, bytesCached, newBytesCached ->
            this@ParallelProgressListener.onProgressAtIndex(
                index,
                contentLength,
                requestLength,
                bytesCached,
                newBytesCached
            )
        }
    }
}