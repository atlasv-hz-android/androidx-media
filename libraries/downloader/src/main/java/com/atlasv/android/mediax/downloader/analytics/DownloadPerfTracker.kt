package com.atlasv.android.mediax.downloader.analytics

/**
 * Created by weiping on 2024/11/12
 */
interface DownloadPerfTracker {
    fun trackDownloadSpeed(bytesPerSecond: Long)
    fun trackDownloadStart()
    fun trackDownloadSuccess(fileSize: Long)
    fun trackSaveSuccess()
    fun trackDownloadFailed(cause: Throwable)
}