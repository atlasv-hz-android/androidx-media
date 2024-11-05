package com.atlasv.android.mediax.downloader.util

/**
 * Created by weiping on 2024/11/5
 */
interface MediaXLogger {
    fun d(messageSupplier: () -> String)
    fun w(messageSupplier: () -> String)
    fun e(messageSupplier: () -> String)
    fun w(cause: Throwable?, messageSupplier: () -> String)
    fun e(cause: Throwable?, messageSupplier: () -> String)
}

