package com.atlasv.android.mediax.downloader.core

import androidx.media3.datasource.cache.CacheWriter.ProgressListener

/**
 * Created by weiping on 2024/9/8
 */
interface DownloadListener {
    fun onProgress(
        requestLength: Long,
        bytesCached: Long,
        newBytesCached: Long,
        downloadUrl: String,
        id: String,
    )
}

fun DownloadListener.asProgressListener(
    uriString: String,
    taskId: String = uriString
): ProgressListener {
    return ProgressListener { requestLength, bytesCached, newBytesCached ->
        this.onProgress(requestLength, bytesCached, newBytesCached, uriString, taskId)
    }
}