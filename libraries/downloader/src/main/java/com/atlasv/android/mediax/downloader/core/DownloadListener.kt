package com.atlasv.android.mediax.downloader.core

import com.atlasv.android.mediax.downloader.model.SpecProgressInfo
import com.atlasv.android.mediax.downloader.output.OutputTarget

/**
 *
 *
 * Created by weiping on 2024/9/8
 */
interface DownloadListener {

    /**
     * [specProgressInfoMap] 各个分片的进度
     */
    fun onProgress(
        requestLength: Long,
        bytesCached: Long,
        newBytesCached: Long,
        speedPerSeconds: Long,
        downloadUrl: String,
        taskId: String,
        specProgressInfoMap: Map<Int, SpecProgressInfo>
    )

    fun onDownloadSpeed(taskId: String, downloadUrl: String, bytesPerSecond: Long, rangeCount: Int)
    fun onDownloadStart(taskId: String, downloadUrl: String)
    fun onDownloadSuccess(taskId: String, downloadUrl: String, rangeCount: Int)
    fun onSaveSuccess(
        taskId: String,
        downloadUrl: String,
        fileSize: Long,
        outputTarget: OutputTarget
    )

    fun onDownloadFailed(taskId: String, downloadUrl: String, cause: Throwable)
}