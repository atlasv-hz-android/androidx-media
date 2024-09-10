package com.atlasv.android.mediax.downloader.core

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