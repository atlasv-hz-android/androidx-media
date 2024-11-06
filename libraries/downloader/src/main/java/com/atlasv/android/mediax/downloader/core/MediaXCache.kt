package com.atlasv.android.mediax.downloader.core

import android.annotation.SuppressLint
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import com.atlasv.android.mediax.downloader.datasource.UpstreamStrategy

/**
 *
 * [缓存媒体](https://developer.android.com/media/media3/exoplayer/network-stacks?hl=zh-cn#caching)
 *
 * Created by weiping on 2024/7/8
 */
@SuppressLint("UnsafeOptInUsageError")
class MediaXCache(
    val cache: Cache,
    private val upstreamStrategy: UpstreamStrategy,
    val cacheKeyFactory: CacheKeyFactory = CacheKeyFactory.DEFAULT,
) {

    val cacheDataSourceFactory by lazy {
        createCacheDataSourceFactory()
    }

    private fun createCacheDataSourceFactory(): CacheDataSource.Factory {
        return CacheDataSource.Factory().setCache(cache).setCacheKeyFactory(cacheKeyFactory)
            .setUpstreamDataSourceFactory(upstreamStrategy.createDataSourceFactory())
    }

    fun createDataSource(): CacheDataSource {
        return cacheDataSourceFactory.createDataSource()
    }
}