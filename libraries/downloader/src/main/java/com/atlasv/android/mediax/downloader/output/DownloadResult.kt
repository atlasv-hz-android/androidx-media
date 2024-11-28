package com.atlasv.android.mediax.downloader.output

/**
 * Created by weiping on 2024/11/28
 */
class DownloadResult(
    val downloadUrl: String,
    val outputTarget: OutputTarget,
    val contentLength: Long
)