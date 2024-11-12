package com.atlasv.android.mediax.downloader.analytics

/**
 * Created by weiping on 2024/11/12
 */
interface DownloadPerfTracker {
    fun trackDownloadSpeed(bytesPerSecond: Long, rangeCount: Int)
    fun trackDownloadStart()
    fun trackDownloadSuccess(rangeCount: Int)
    fun trackSaveSuccess(fileSize: Long)
    fun trackDownloadFailed(cause: Throwable)
}