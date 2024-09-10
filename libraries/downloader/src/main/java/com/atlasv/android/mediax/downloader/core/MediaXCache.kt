package com.atlasv.android.mediax.downloader.core

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient

/**
 * Created by weiping on 2024/7/8
 */
@SuppressLint("UnsafeOptInUsageError")
class MediaXCache(
    private val appContext: Context,
    val cache: Cache,
    private val okhttpClient: OkHttpClient,
    val cacheKeyFactory: CacheKeyFactory = CacheKeyFactory.DEFAULT
) {

    private fun createHttpDataSourceFactory(): DefaultDataSource.Factory {
        return DefaultDataSource.Factory(
            appContext, OkHttpDataSource.Factory(okhttpClient)
        )
    }

    val cacheDataSourceFactory by lazy {
        createCacheDataSourceFactory()
    }

    private fun createCacheDataSourceFactory(): CacheDataSource.Factory {
        return CacheDataSource.Factory().setCache(cache).setCacheKeyFactory(cacheKeyFactory)
            .setUpstreamDataSourceFactory(createHttpDataSourceFactory())
    }

    fun createDataSource(): CacheDataSource {
        return cacheDataSourceFactory.createDataSource()
    }
}