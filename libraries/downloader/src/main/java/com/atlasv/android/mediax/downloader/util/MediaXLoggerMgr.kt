package com.atlasv.android.mediax.downloader.util

import androidx.media3.common.util.MediaXLogger

/**
 * Created by weiping on 2024/11/5
 */
object MediaXLoggerMgr {
    var loggerFactory: MediaXLoggerFactory? = null
    val mediaXLogger: MediaXLogger? by lazy {
        loggerFactory?.createLogger("mediaX-downloader")
    }
    val mediaXCacheWriterLogger: MediaXLogger? by lazy {
        loggerFactory?.createLogger("mediaX-downloader-cache-writer")
    }
}
