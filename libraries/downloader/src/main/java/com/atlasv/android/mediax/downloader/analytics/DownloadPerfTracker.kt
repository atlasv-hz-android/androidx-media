package com.atlasv.android.mediax.downloader.analytics

/**
 * Created by weiping on 2024/11/12
 */
interface DownloadPerfTracker {
    fun trackDownloadSpeed(downloadUrl: String, bytesPerSecond: Long, rangeCount: Int)
    fun trackDownloadStart(downloadUrl: String)
    fun trackDownloadSuccess(downloadUrl: String, rangeCount: Int)
    fun trackSaveSuccess(downloadUrl: String, fileSize: Long)
    fun trackDownloadFailed(downloadUrl: String, cause: Throwable)
}