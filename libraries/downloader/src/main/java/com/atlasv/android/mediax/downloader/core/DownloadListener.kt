package com.atlasv.android.mediax.downloader.core

import com.atlasv.android.mediax.downloader.model.SpecProgressInfo

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
        id: String,
        specProgressInfoMap: Map<Int, SpecProgressInfo>
    )
}