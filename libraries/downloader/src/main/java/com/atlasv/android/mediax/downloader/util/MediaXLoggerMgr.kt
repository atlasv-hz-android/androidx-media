package com.atlasv.android.mediax.downloader.util

import com.android.now.logger.ILogger

/**
 * Created by weiping on 2024/11/5
 */
object MediaXLoggerMgr {
    var loggerFactory: MediaXLoggerFactory? = null
    val mediaXLogger: ILogger? by lazy {
        loggerFactory?.createLogger("mediaX-downloader")
    }
    val mediaXCacheWriterLogger: ILogger? by lazy {
        loggerFactory?.createLogger("mediaX-downloader-cache-writer")
    }
}
