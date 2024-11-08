package com.atlasv.android.media3.demo.download

import com.atlasv.android.mediax.downloader.model.SpecProgressInfo

/**
 * Created by weiping on 2024/11/8
 */
data class ProgressItem(
    val downloadUrl: String,
    val requestLength: Long,
    val bytesCached: Long,
    val specs: List<SpecProgressInfo>
) {
    val progress = if (requestLength <= 0) 0f else (bytesCached.toFloat() / requestLength)
}
