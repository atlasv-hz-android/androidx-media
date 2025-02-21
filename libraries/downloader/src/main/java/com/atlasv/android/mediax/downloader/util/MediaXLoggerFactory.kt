package com.atlasv.android.mediax.downloader.util

import androidx.media3.common.util.MediaXLogger

/**
 * Created by weiping on 2025/2/21
 */
interface MediaXLoggerFactory {
    fun createLogger(tag: String): MediaXLogger
}