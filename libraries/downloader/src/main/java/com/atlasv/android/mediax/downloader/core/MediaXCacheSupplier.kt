package com.atlasv.android.mediax.downloader.core

/**
 * Created by weiping on 2024/9/10
 */
interface MediaXCacheSupplier {
    fun get(): MediaXCache
}