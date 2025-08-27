package com.android.now.mediax.downloader.listener

import com.android.now.mediax.downloader.core.DownloadListener
import com.android.now.mediax.downloader.model.SpecProgressInfo
import com.android.now.mediax.downloader.output.OutputTarget

/**
 * Created by weiping on 2024/12/24
 */
class DownloadListenerParent(private val children: List<DownloadListener>) : DownloadListener {
    override fun onProgress(
        requestLength: Long,
        bytesCached: Long,
        newBytesCached: Long,
        speedPerSeconds: Long,
        downloadUrl: String,
        taskId: String,
        specProgressInfoMap: Map<Int, SpecProgressInfo>
    ) {
        children.forEach { child ->
            child.onProgress(
                requestLength,
                bytesCached,
                newBytesCached,
                speedPerSeconds,
                downloadUrl,
                taskId,
                specProgressInfoMap
            )
        }

    }

    override fun onDownloadSpeed(
        taskId: String,
        downloadUrl: String,
        bytesPerSecond: Long,
        rangeCount: Int
    ) {
        children.forEach { child ->
            child.onDownloadSpeed(taskId, downloadUrl, bytesPerSecond, rangeCount)
        }
    }

    override fun onDownloadStart(taskId: String, downloadUrl: String) {
        children.forEach { child ->
            child.onDownloadStart(taskId, downloadUrl)
        }
    }

    override fun onDownloadRestart(taskId: String, downloadUrl: String) {
        children.forEach { child ->
            child.onDownloadRestart(taskId, downloadUrl)
        }
    }

    override fun onDownloadSuccess(
        taskId: String,
        downloadUrl: String,
        rangeCount: Int,
        isNewTask: Boolean
    ) {
        children.forEach { child ->
            child.onDownloadSuccess(taskId, downloadUrl, rangeCount, isNewTask)
        }
    }

    override fun onSaveSuccess(
        taskId: String,
        downloadUrl: String,
        fileSize: Long,
        outputTarget: OutputTarget,
        isNewTask: Boolean
    ) {
        children.forEach { child ->
            child.onSaveSuccess(taskId, downloadUrl, fileSize, outputTarget, isNewTask)
        }
    }

    override fun onDownloadFailed(taskId: String, downloadUrl: String, cause: Throwable) {
        children.forEach { child ->
            child.onDownloadFailed(taskId, downloadUrl, cause)
        }
    }
}

fun DownloadListener?.withParent(parent: DownloadListener?): DownloadListener? {
    parent ?: return this
    this ?: return parent
    return DownloadListenerParent(listOf(parent, this))
}