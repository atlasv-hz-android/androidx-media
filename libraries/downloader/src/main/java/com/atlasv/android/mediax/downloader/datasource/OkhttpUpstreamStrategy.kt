package com.atlasv.android.mediax.downloader.datasource

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.atlasv.android.mediax.downloader.core.MediaXConstants
import okhttp3.OkHttpClient

/**
 * Created by weiping on 2024/11/6
 */
class OkhttpUpstreamStrategy(
    private val appContext: Context,
    private val okhttpClient: OkHttpClient,
    private val userAgent: String = MediaXConstants.DEFAULT_OKHTTP_USER_AGENT
) : UpstreamStrategy {
    override fun createDataSourceFactory(): DataSource.Factory {
        return DefaultDataSource.Factory(
            appContext,
            OkHttpDataSource.Factory(okhttpClient).setUserAgent(userAgent)
        )
    }
}