package com.atlasv.android.mediax.downloader.util

import com.atlasv.android.logger.ILogger

/**
 * Created by weiping on 2025/2/21
 */
interface MediaXLoggerFactory {
    fun createLogger(tag: String): ILogger
}