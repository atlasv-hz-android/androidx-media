package com.atlasv.android.mediax.downloader.feature

import com.google.common.net.MediaType

/**
 * * [downloadUrl] 下载链接
 * * [mediaType] 下载媒体类型
 * * [attachAudioUrl] 有的视频链接没有音频，这里附上对应的音频链接，以便在下载完成之后，检测到没有音频之后进行合并
 *
 * Created by weiping on 2025/1/13
 */
data class DownloadRequest(
    val downloadUrl: String,
    val taskId: String,
    val mediaType: MediaType,
    val attachAudioUrl: String?,
    var httpRequestHeaders: Map<String, String>? = null
)