package com.android.now.mediax.downloader.datasource

import androidx.media3.datasource.DataSource

/**
 * Created by weiping on 2024/11/6
 */
interface UpstreamStrategy {
    fun createDataSourceFactory(): DataSource.Factory
}