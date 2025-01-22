package com.atlasv.android.mediax.downloader.listener

import com.atlasv.android.mediax.downloader.core.DownloadListener
import com.atlasv.android.mediax.downloader.model.SpecProgressInfo
import com.atlasv.android.mediax.downloader.output.OutputTarget

/**
 * Created by weiping on 2024/12/24
 */
open class SimpleDownloadListener : DownloadListener {
    override fun onProgress(
        requestLength: Long,
        bytesCached: Long,
        newBytesCached: Long,
        speedPerSeconds: Long,
        downloadUrl: String,
        taskId: String,
        specProgressInfoMap: Map<Int, SpecProgressInfo>
    ) {

    }

    override fun onDownloadSpeed(
        taskId: String,
        downloadUrl: String,
        bytesPerSecond: Long,
        rangeCount: Int
    ) {
    }

    override fun onDownloadStart(taskId: String, downloadUrl: String) {
    }

    override fun onDownloadRestart(taskId: String, downloadUrl: String) {
    }

    override fun onDownloadSuccess(taskId: String, downloadUrl: String, rangeCount: Int) {
    }

    override fun onSaveSuccess(
        taskId: String,
        downloadUrl: String,
        fileSize: Long,
        outputTarget: OutputTarget
    ) {
    }

    override fun onDownloadFailed(taskId: String, downloadUrl: String, cause: Throwable) {
    }
}