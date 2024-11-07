package com.atlasv.android.mediax.downloader.cache

import androidx.media3.datasource.cache.CacheWriter.ProgressListener

/**
 * Created by weiping on 2024/11/5
 */
class ParallelProgressListener(private val callback: ProgressListener?) {
    private val mergeProgressInfo = mutableMapOf<Int, Pair<Long, Long>>()

    private fun onProgressAtIndex(
        index: Int,
        contentLength: Long,
        requestLength: Long,
        bytesCached: Long,
        newBytesCached: Long
    ) {
        mergeProgressInfo[index] = mergeProgressInfo[index]?.copy(
            first = bytesCached, second = requestLength
        ) ?: (bytesCached to requestLength)
        val (mergeBytesCached, mergeContentLength) = calcMergeProgress(
            contentLength,
            mergeProgressInfo
        )
        callback?.onProgress(
            mergeContentLength,
            mergeBytesCached,
            newBytesCached
        )
    }

    private fun calcMergeProgress(
        contentLength: Long,
        infoMap: Map<Int, Pair<Long, Long>>
    ): Pair<Long, Long> {
        val values: Collection<Pair<Long, Long>> = infoMap.values.toList()
        val validContentLength = contentLength.takeIf { it > 0 } ?: values.sumOf {
            it.second
        }
        val mergeBytesCached = values.sumOf {
            it.first
        }
        return mergeBytesCached to validContentLength
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