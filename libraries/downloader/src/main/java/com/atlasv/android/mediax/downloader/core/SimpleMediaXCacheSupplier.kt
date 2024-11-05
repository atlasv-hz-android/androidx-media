package com.atlasv.android.mediax.downloader.core

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Created by weiping on 2024/11/5
 */
class SimpleMediaXCacheSupplier(
    private val appContext: Context,
    private val okhttpClient: OkHttpClient,
    private val cacheDirSupplier: () -> File,
    private val evictor: CacheEvictor = NoOpCacheEvictor(),
    private val cacheKeyFactory: CacheKeyFactory = CacheKeyFactory.DEFAULT
) : MediaXCacheSupplier {
    private val mediaXCache: MediaXCache by lazy {
        val dir = cacheDirSupplier.invoke().also {
            it.mkdirs()
        }
        val cache = SimpleCache(
            dir,
            evictor,
            StandaloneDatabaseProvider(appContext),
        )
        MediaXCache(
            appContext, cache, okhttpClient, cacheKeyFactory = cacheKeyFactory
        )
    }

    override fun get(): MediaXCache {
        return mediaXCache
    }
}