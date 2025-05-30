package com.android.now.mediax.downloader.model

/**
 * Created by weiping on 2024/11/8
 */
data class SpecProgressInfo(val specIndex: Int, val requestLength: Long, val bytesCached: Long) {
    val progress = if (requestLength <= 0) 0f else (bytesCached.toFloat() / requestLength)
}
