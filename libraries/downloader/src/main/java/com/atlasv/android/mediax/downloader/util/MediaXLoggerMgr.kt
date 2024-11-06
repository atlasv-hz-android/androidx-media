package com.atlasv.android.mediax.downloader.util

/**
 * Created by weiping on 2024/11/5
 */
object MediaXLoggerMgr {
    var loggerSupplier: (() -> MediaXLogger)? = null
    val mediaXLogger get() = loggerSupplier?.invoke()
}
